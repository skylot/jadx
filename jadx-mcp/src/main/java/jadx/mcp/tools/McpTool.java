package jadx.mcp.tools;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;

/**
 * MCP tool definition
 */
public final class McpTool {
	private final String name;
	private final String title;
	private final String description;
	private final JsonObject inputSchema;
	private final @Nullable JsonObject outputSchema;
	private final boolean readOnly;
	private final boolean destructive;
	private final boolean idempotent;
	private final ToolHandler handler;

	private McpTool(Builder b) {
		this.name = Objects.requireNonNull(b.name);
		this.title = b.title != null ? b.title : b.name;
		this.description = Objects.requireNonNull(b.description);
		this.inputSchema = b.inputSchema != null ? b.inputSchema : Schema.object().build();
		this.outputSchema = b.outputSchema;
		this.readOnly = b.readOnly;
		this.destructive = b.destructive;
		this.idempotent = b.idempotent;
		this.handler = Objects.requireNonNull(b.handler);
	}

	public static Builder builder(String name) {
		return new Builder(name);
	}

	public String getName() {
		return name;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public JsonObject getInputSchema() {
		return inputSchema;
	}

	public @Nullable JsonObject getOutputSchema() {
		return outputSchema;
	}

	/**
	 * Tool doesn't modify project data
	 */
	public boolean isReadOnly() {
		return readOnly;
	}

	public boolean isDestructive() {
		return destructive;
	}

	public boolean isIdempotent() {
		return idempotent;
	}

	public ToolHandler getHandler() {
		return handler;
	}

	/**
	 * Tool definition for 'tools/list' response
	 */
	public JsonObject toJson(boolean includeOutputSchema) {
		JsonObject obj = new JsonObject();
		obj.addProperty("name", name);
		obj.addProperty("title", title);
		obj.addProperty("description", description);
		obj.add("inputSchema", inputSchema);
		if (includeOutputSchema && outputSchema != null) {
			obj.add("outputSchema", outputSchema);
		}
		JsonObject annotations = new JsonObject();
		annotations.addProperty("title", title);
		annotations.addProperty("readOnlyHint", readOnly);
		if (!readOnly) {
			annotations.addProperty("destructiveHint", destructive);
		}
		annotations.addProperty("idempotentHint", idempotent);
		annotations.addProperty("openWorldHint", false);
		obj.add("annotations", annotations);
		return obj;
	}

	public static final class Builder {
		private final String name;
		private @Nullable String title;
		private @Nullable String description;
		private @Nullable JsonObject inputSchema;
		private @Nullable JsonObject outputSchema;
		private boolean readOnly = true;
		private boolean destructive = false;
		private boolean idempotent = true;
		private @Nullable ToolHandler handler;

		private Builder(String name) {
			this.name = name;
		}

		public Builder title(String title) {
			this.title = title;
			return this;
		}

		public Builder description(String description) {
			this.description = description;
			return this;
		}

		public Builder input(JsonObject schema) {
			this.inputSchema = schema;
			return this;
		}

		public Builder output(JsonObject schema) {
			this.outputSchema = schema;
			return this;
		}

		/**
		 * Mark tool as modifying project data (renames, comments, project save)
		 */
		public Builder write(boolean destructive, boolean idempotent) {
			this.readOnly = false;
			this.destructive = destructive;
			this.idempotent = idempotent;
			return this;
		}

		public Builder handler(ToolHandler handler) {
			this.handler = handler;
			return this;
		}

		public McpTool build() {
			return new McpTool(this);
		}
	}
}
