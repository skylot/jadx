package jadx.gui.ai;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import jadx.core.utils.exceptions.JadxRuntimeException;

/**
 * Minimal client for an OpenAI-compatible "chat/completions" HTTP API.
 * Network access (proxy, extra trusted CA certificate) is configured manually
 * from {@link AiSettings}, jadx does not auto-detect system proxies or filtering software.
 */
public class AiClient {
	private static final Logger LOG = LoggerFactory.getLogger(AiClient.class);
	private static final Gson GSON = new Gson();
	private static final Duration TIMEOUT = Duration.ofSeconds(60);

	private final AiSettings settings;

	public AiClient(AiSettings settings) {
		this.settings = settings;
	}

	/**
	 * Blocking call, must be executed on a background thread.
	 */
	public String sendMessage(List<AiChatMessage> messages) throws IOException, InterruptedException {
		String baseUrl = trimTrailingSlash(settings.getBaseUrl());
		if (baseUrl.isEmpty()) {
			throw new JadxRuntimeException("AI Assistant: base URL is not set");
		}
		if (settings.getApiKey().isEmpty()) {
			throw new JadxRuntimeException("AI Assistant: API key is not set");
		}
		String url = baseUrl + "/chat/completions";

		ChatRequest chatRequest = new ChatRequest();
		chatRequest.model = settings.getModel();
		chatRequest.messages = messages.stream()
				.map(m -> new ChatRequestMessage(m.getRole(), m.getContent()))
				.collect(Collectors.toList());
		String requestBody = GSON.toJson(chatRequest);

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.timeout(TIMEOUT)
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + settings.getApiKey())
				.POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
				.build();

		HttpClient client = buildHttpClient();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		int status = response.statusCode();
		String body = response.body();
		if (status < 200 || status >= 300) {
			throw new IOException("AI request failed with HTTP " + status + ": " + shorten(body));
		}
		return parseResponse(body);
	}

	private static String shorten(@Nullable String s) {
		if (s == null) {
			return "";
		}
		return s.length() > 500 ? s.substring(0, 500) + "..." : s;
	}

	private String parseResponse(String body) throws IOException {
		try {
			JsonObject root = GSON.fromJson(body, JsonObject.class);
			if (root.has("error")) {
				throw new IOException("AI API error: " + root.get("error").toString());
			}
			var choices = root.getAsJsonArray("choices");
			if (choices == null || choices.isEmpty()) {
				throw new IOException("AI response has no choices: " + shorten(body));
			}
			JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
			if (message == null || !message.has("content")) {
				throw new IOException("AI response message has no content: " + shorten(body));
			}
			return message.get("content").getAsString();
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("Failed to parse AI response: " + shorten(body), e);
		}
	}

	private HttpClient buildHttpClient() {
		HttpClient.Builder builder = HttpClient.newBuilder()
				.connectTimeout(TIMEOUT)
				.followRedirects(HttpClient.Redirect.NORMAL);

		String proxyHost = settings.getProxyHost();
		if (proxyHost != null && !proxyHost.isBlank()) {
			int proxyPort = parsePort(settings.getProxyPort());
			builder.proxy(ProxySelector.of(new InetSocketAddress(proxyHost.trim(), proxyPort)));
			String proxyUser = settings.getProxyUsername();
			if (proxyUser != null && !proxyUser.isBlank()) {
				String proxyPass = settings.getProxyPassword();
				builder.authenticator(new Authenticator() {
					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						if (getRequestorType() == RequestorType.PROXY) {
							return new PasswordAuthentication(proxyUser, proxyPass == null ? new char[0] : proxyPass.toCharArray());
						}
						return null;
					}
				});
			}
		}

		String caCertPath = settings.getCustomCaCertPath();
		if (caCertPath != null && !caCertPath.isBlank()) {
			builder.sslContext(buildSslContextWithExtraCa(caCertPath.trim()));
		}
		return builder.build();
	}

	private static int parsePort(@Nullable String portStr) {
		if (portStr == null || portStr.isBlank()) {
			throw new JadxRuntimeException("AI Assistant: proxy port is not set");
		}
		try {
			return Integer.parseInt(portStr.trim());
		} catch (NumberFormatException e) {
			throw new JadxRuntimeException("AI Assistant: invalid proxy port: " + portStr);
		}
	}

	/**
	 * Builds an SSLContext that trusts both the JVM default CA set and an additional
	 * user-provided CA certificate (needed for network filters/proxies that perform
	 * TLS interception with their own root certificate, e.g. parental-control/content filters).
	 */
	private static SSLContext buildSslContextWithExtraCa(String caCertPath) {
		try {
			TrustManagerFactory defaultTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			defaultTmf.init((KeyStore) null);
			X509TrustManager defaultTm = findX509TrustManager(defaultTmf);

			KeyStore extraKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			extraKeyStore.load(null, null);
			try (InputStream in = new FileInputStream(caCertPath)) {
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				int i = 0;
				for (Certificate cert : cf.generateCertificates(in)) {
					extraKeyStore.setCertificateEntry("ai-custom-ca-" + (i++), cert);
				}
			}
			TrustManagerFactory extraTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			extraTmf.init(extraKeyStore);
			X509TrustManager extraTm = findX509TrustManager(extraTmf);

			X509TrustManager combined = new CompositeTrustManager(defaultTm, extraTm);
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[] { combined }, new SecureRandom());
			return sslContext;
		} catch (Exception e) {
			throw new JadxRuntimeException("AI Assistant: failed to load custom CA certificate from: " + caCertPath, e);
		}
	}

	private static X509TrustManager findX509TrustManager(TrustManagerFactory tmf) {
		for (TrustManager tm : tmf.getTrustManagers()) {
			if (tm instanceof X509TrustManager) {
				return (X509TrustManager) tm;
			}
		}
		throw new JadxRuntimeException("No X509TrustManager found");
	}

	private static String trimTrailingSlash(@Nullable String url) {
		if (url == null) {
			return "";
		}
		String trimmed = url.trim();
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed;
	}

	private static final class CompositeTrustManager implements X509TrustManager {
		private final X509TrustManager first;
		private final X509TrustManager second;

		private CompositeTrustManager(X509TrustManager first, X509TrustManager second) {
			this.first = first;
			this.second = second;
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			first.checkClientTrusted(chain, authType);
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			try {
				first.checkServerTrusted(chain, authType);
			} catch (CertificateException e) {
				second.checkServerTrusted(chain, authType);
			}
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			List<X509Certificate> result = new ArrayList<>(List.of(first.getAcceptedIssuers()));
			result.addAll(List.of(second.getAcceptedIssuers()));
			return result.toArray(new X509Certificate[0]);
		}
	}

	private static final class ChatRequest {
		String model;
		List<ChatRequestMessage> messages;
		@SerializedName("stream")
		final boolean stream = false;
	}

	private static final class ChatRequestMessage {
		final String role;
		final String content;

		private ChatRequestMessage(String role, String content) {
			this.role = role;
			this.content = content;
		}
	}
}
