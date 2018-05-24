package jadx.gui.settings;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import say.swing.JFontChooser;

import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;

public class JadxSettingsWindow extends JDialog {
	private static final long serialVersionUID = -1804570470377354148L;

	private static final Logger LOG = LoggerFactory.getLogger(JadxSettingsWindow.class);

	private final transient MainWindow mainWindow;
	private final transient JadxSettings settings;
	private final transient String startSettings;

	private transient boolean needReload = false;

	public JadxSettingsWindow(MainWindow mainWindow, JadxSettings settings) {
		this.mainWindow = mainWindow;
		this.settings = settings;
		this.startSettings = JadxSettingsAdapter.makeString(settings);

		initUI();

		setTitle(NLS.str("preferences.title"));
		setSize(400, 550);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.APPLICATION_MODAL);
		pack();
	}

	private void initUI() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.add(makeDeobfuscationGroup());
		panel.add(makeDecompilationGroup());
		panel.add(makeEditorGroup());
		panel.add(makeOtherGroup());

		JButton saveBtn = new JButton(NLS.str("preferences.save"));
		saveBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				settings.sync();
				if (needReload) {
					mainWindow.reOpenFile();
				}
				dispose();
			}
		});
		JButton cancelButton = new JButton(NLS.str("preferences.cancel"));
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				JadxSettingsAdapter.fill(settings, startSettings);
				dispose();
			}
		});

		JButton resetBtn = new JButton(NLS.str("preferences.reset"));
		resetBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				int res = JOptionPane.showConfirmDialog(
						JadxSettingsWindow.this,
						NLS.str("preferences.reset_message"),
						NLS.str("preferences.reset_title"),
						JOptionPane.YES_NO_OPTION);
				if (res == JOptionPane.YES_OPTION) {
					String defaults = JadxSettingsAdapter.makeString(new JadxSettings());
					JadxSettingsAdapter.fill(settings, defaults);
					getContentPane().removeAll();
					initUI();
					pack();
					repaint();
				}
			}
		});

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(resetBtn);
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(saveBtn);
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPane.add(cancelButton);

		Container contentPane = getContentPane();
		contentPane.add(panel, BorderLayout.CENTER);
		contentPane.add(buttonPane, BorderLayout.PAGE_END);
		getRootPane().setDefaultButton(saveBtn);
	}

	private SettingsGroup makeDeobfuscationGroup() {
		JCheckBox deobfOn = new JCheckBox();
		deobfOn.setSelected(settings.isDeobfuscationOn());
		deobfOn.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				settings.setDeobfuscationOn(e.getStateChange() == ItemEvent.SELECTED);
				needReload();
			}
		});

		JCheckBox deobfForce = new JCheckBox();
		deobfForce.setSelected(settings.isDeobfuscationForceSave());
		deobfForce.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				settings.setDeobfuscationForceSave(e.getStateChange() == ItemEvent.SELECTED);
				needReload();
			}
		});

		final JSpinner minLen = new JSpinner();
		minLen.setValue(settings.getDeobfuscationMinLength());
		minLen.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				settings.setDeobfuscationMinLength((Integer) minLen.getValue());
				needReload();
			}
		});

		final JSpinner maxLen = new JSpinner();
		maxLen.setValue(settings.getDeobfuscationMaxLength());
		maxLen.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				settings.setDeobfuscationMaxLength((Integer) maxLen.getValue());
				needReload();
			}
		});

		JCheckBox deobfSourceAlias = new JCheckBox();
		deobfSourceAlias.setSelected(settings.isDeobfuscationUseSourceNameAsAlias());
		deobfSourceAlias.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				settings.setDeobfuscationUseSourceNameAsAlias(e.getStateChange() == ItemEvent.SELECTED);
				needReload();
			}
		});

		SettingsGroup deobfGroup = new SettingsGroup(NLS.str("preferences.deobfuscation"));
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_on"), deobfOn);
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_force"), deobfForce);
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_min_len"), minLen);
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_max_len"), maxLen);
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_source_alias"), deobfSourceAlias);
		deobfGroup.end();

		Collection<JComponent> connectedComponents = Arrays.asList(deobfForce, minLen, maxLen, deobfSourceAlias);
		deobfOn.addItemListener(e -> enableComponentList(connectedComponents, e.getStateChange() == ItemEvent.SELECTED));
		enableComponentList(connectedComponents, settings.isDeobfuscationOn());
		return deobfGroup;
	}

	private void enableComponentList(Collection<JComponent> connectedComponents, boolean enabled) {
		connectedComponents.forEach(comp -> comp.setEnabled(enabled));
	}

	private SettingsGroup makeEditorGroup() {
		JButton fontBtn = new JButton(NLS.str("preferences.select_font"));
		fontBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				JFontChooser fontChooser = new JFontChooser();
				fontChooser.setSelectedFont(settings.getFont());
				int result = fontChooser.showDialog(JadxSettingsWindow.this);
				if (result == JFontChooser.OK_OPTION) {
					Font font = fontChooser.getSelectedFont();
					LOG.info("Selected Font : {}", font);
					settings.setFont(font);
					mainWindow.updateFont(font);
				}
			}
		});

		SettingsGroup other = new SettingsGroup(NLS.str("preferences.editor"));
		other.addRow(NLS.str("preferences.font"), fontBtn);
		return other;
	}

	private SettingsGroup makeDecompilationGroup() {
		JCheckBox fallback = new JCheckBox();
		fallback.setSelected(settings.isFallbackMode());
		fallback.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				settings.setFallbackMode(e.getStateChange() == ItemEvent.SELECTED);
				needReload();
			}
		});

		JCheckBox showInconsistentCode = new JCheckBox();
		showInconsistentCode.setSelected(settings.isShowInconsistentCode());
		showInconsistentCode.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				settings.setShowInconsistentCode(e.getStateChange() == ItemEvent.SELECTED);
				needReload();
			}
		});

		JCheckBox resourceDecode = new JCheckBox();
		resourceDecode.setSelected(settings.isSkipResources());
		resourceDecode.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				settings.setSkipResources(e.getStateChange() == ItemEvent.SELECTED);
				needReload();
			}
		});

		SpinnerNumberModel spinnerModel = new SpinnerNumberModel(
				settings.getThreadsCount(), 1, Runtime.getRuntime().availableProcessors() * 2, 1);
		final JSpinner threadsCount = new JSpinner(spinnerModel);
		threadsCount.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				settings.setThreadsCount((Integer) threadsCount.getValue());
				needReload();
			}
		});

		JCheckBox autoStartJobs = new JCheckBox();
		autoStartJobs.setSelected(settings.isAutoStartJobs());
		autoStartJobs.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				settings.setAutoStartJobs(e.getStateChange() == ItemEvent.SELECTED);
			}
		});

		JCheckBox escapeUnicode = new JCheckBox();
		escapeUnicode.setSelected(settings.escapeUnicode());
		escapeUnicode.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				settings.setEscapeUnicode(e.getStateChange() == ItemEvent.SELECTED);
				needReload();
			}
		});

		JCheckBox replaceConsts = new JCheckBox();
		replaceConsts.setSelected(settings.isReplaceConsts());
		replaceConsts.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				settings.setReplaceConsts(e.getStateChange() == ItemEvent.SELECTED);
				needReload();
			}
		});

		SettingsGroup other = new SettingsGroup(NLS.str("preferences.decompile"));
		other.addRow(NLS.str("preferences.threads"), threadsCount);
		other.addRow(NLS.str("preferences.start_jobs"), autoStartJobs);
		other.addRow(NLS.str("preferences.showInconsistentCode"), showInconsistentCode);
		other.addRow(NLS.str("preferences.escapeUnicode"), escapeUnicode);
		other.addRow(NLS.str("preferences.replaceConsts"), replaceConsts);
		other.addRow(NLS.str("preferences.fallback"), fallback);
		other.addRow(NLS.str("preferences.skipResourcesDecode"), resourceDecode);
		return other;
	}

	private SettingsGroup makeOtherGroup() {
		JCheckBox update = new JCheckBox();
		update.setSelected(settings.isCheckForUpdates());
		update.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				settings.setCheckForUpdates(e.getStateChange() == ItemEvent.SELECTED);
			}
		});

		JCheckBox cfg = new JCheckBox();
		cfg.setSelected(settings.isCfgOutput());
		cfg.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				settings.setCfgOutput(e.getStateChange() == ItemEvent.SELECTED);
				needReload();
			}
		});

		JCheckBox rawCfg = new JCheckBox();
		rawCfg.setSelected(settings.isRawCfgOutput());
		rawCfg.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				settings.setRawCfgOutput(e.getStateChange() == ItemEvent.SELECTED);
				needReload();
			}
		});

		SettingsGroup other = new SettingsGroup(NLS.str("preferences.other"));
		other.addRow(NLS.str("preferences.check_for_updates"), update);
		other.addRow(NLS.str("preferences.cfg"), cfg);
		other.addRow(NLS.str("preferences.raw_cfg"), rawCfg);
		return other;
	}

	private void needReload() {
		needReload = true;
	}

	private static class SettingsGroup extends JPanel {
		private static final long serialVersionUID = -6487309975896192544L;

		private final GridBagConstraints c;
		private int row;

		public SettingsGroup(String title) {
			setBorder(BorderFactory.createTitledBorder(title));
			setLayout(new GridBagLayout());
			c = new GridBagConstraints();
			c.insets = new Insets(5, 5, 5, 5);
			c.weighty = 1.0;
		}

		public void addRow(String label, JComponent comp) {
			c.gridy = row++;
			JLabel jLabel = new JLabel(label);
			jLabel.setLabelFor(comp);
			jLabel.setHorizontalAlignment(SwingConstants.LEFT);
			c.gridx = 0;
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.LINE_START;
			c.weightx = 0.8;
			c.fill = GridBagConstraints.NONE;
			add(jLabel, c);
			c.gridx = 1;
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.anchor = GridBagConstraints.CENTER;
			c.weightx = 0.2;
			c.fill = GridBagConstraints.HORIZONTAL;
			add(comp, c);

			comp.addPropertyChangeListener("enabled", evt -> jLabel.setEnabled((boolean) evt.getNewValue()));
		}

		public void end() {
			add(Box.createVerticalGlue());
		}
	}
}
