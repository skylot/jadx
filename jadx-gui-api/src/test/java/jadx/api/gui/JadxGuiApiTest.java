package jadx.api.gui;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import org.junit.jupiter.api.Test;

import jadx.api.ICodeInfo;
import jadx.api.gui.dock.DockArea;
import jadx.api.gui.dock.DockableOptions;
import jadx.api.gui.dock.events.DockableHidden;
import jadx.api.gui.dock.events.DockableShown;
import jadx.api.gui.editor.events.EditorClosed;
import jadx.api.gui.editor.events.EditorOpened;
import jadx.api.gui.editor.events.EditorSelected;
import jadx.api.gui.tree.BaseTreeNodeEnum;
import jadx.api.gui.tree.IJadxTree;
import jadx.api.gui.tree.ITreeNode;
import jadx.api.impl.SimpleCodeInfo;
import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeNodeRef;
import jadx.gui.code.HtmlContentPanel;
import jadx.gui.tree.JadxCodeTreeNode;
import jadx.gui.tree.JadxEditableTreeNode;
import jadx.gui.tree.JadxTreeNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JadxGuiApiTest {

	@Test
	void dockableOptionsDefaultsValidationAndFluentSetters() {
		DockableOptions defaults = new DockableOptions("sample", "Sample");
		assertThat(defaults.getId()).isEqualTo("sample");
		assertThat(defaults.getTitle()).isEqualTo("Sample");
		assertThat(defaults.getDefaultArea()).isEqualTo(DockArea.BOTTOM);
		assertThat(defaults.getIcon()).isNull();
		assertThat(defaults.isClosable()).isTrue();
		assertThat(defaults.isShowOnStartup()).isFalse();
		assertThat(defaults.isMenuEntry()).isTrue();
		assertThat(defaults.getKeyBinding()).isNull();

		ImageIcon icon = new ImageIcon();
		DockableOptions custom = new DockableOptions("panel-1", "Panel Title")
				.defaultArea(DockArea.RIGHT)
				.icon(icon)
				.closable(false)
				.showOnStartup(true)
				.menuEntry(false)
				.keyBinding("ctrl shift P");
		assertThat(custom.getDefaultArea()).isEqualTo(DockArea.RIGHT);
		assertThat(custom.getIcon()).isSameAs(icon);
		assertThat(custom.isClosable()).isFalse();
		assertThat(custom.isShowOnStartup()).isTrue();
		assertThat(custom.isMenuEntry()).isFalse();
		assertThat(custom.getKeyBinding()).isEqualTo("ctrl shift P");

		assertThatThrownBy(() -> new DockableOptions("Bad_Id", "Title"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new DockableOptions("valid-id", ""))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void eventTypesAndAccessors() {
		DockableShown shown = new DockableShown("plugin:panel");
		assertThat(shown.getType()).isSameAs(JadxGuiEvents.DOCKABLE_SHOWN);
		assertThat(shown.getDockableId()).isEqualTo("plugin:panel");
		assertThat(shown.toString()).contains("plugin:panel");

		DockableHidden hidden = new DockableHidden("plugin:panel");
		assertThat(hidden.getType()).isSameAs(JadxGuiEvents.DOCKABLE_HIDDEN);
		assertThat(hidden.getDockableId()).isEqualTo("plugin:panel");

		ICodeNodeRef nodeRef = stubCodeRef();
		EditorOpened opened = new EditorOpened(nodeRef);
		assertThat(opened.getType()).isSameAs(JadxGuiEvents.EDITOR_OPENED);
		assertThat(opened.getNode()).isSameAs(nodeRef);

		EditorSelected selectedNull = new EditorSelected(null);
		assertThat(selectedNull.getType()).isSameAs(JadxGuiEvents.EDITOR_SELECTED);
		assertThat(selectedNull.getNode()).isNull();

		EditorClosed closed = new EditorClosed(nodeRef);
		assertThat(closed.getType()).isSameAs(JadxGuiEvents.EDITOR_CLOSED);
		assertThat(closed.getNode()).isSameAs(nodeRef);
	}

	@Test
	void editableTreeNodeExtendsJadxTreeNodeAndNotifiesListeners() {
		List<Boolean> events = new ArrayList<>();
		JadxEditableTreeNode editable = new JadxEditableTreeNode() {
			@Override
			public String getName() {
				return "custom-editable";
			}

			@Override
			public void save(String newContent) {
			}
		};

		assertThat(editable).isInstanceOf(JadxTreeNode.class);
		assertThat(editable.isChanged()).isFalse();
		editable.addChangeListener(events::add);
		editable.setChanged(false);
		editable.setChanged(true);
		editable.setChanged(true);
		editable.setChanged(false);
		assertThat(events).containsExactly(false, true, false);
		assertThat(editable.toString()).isEqualTo("custom-editable");
		assertThat(editable.getContentPanel(new StubMainWindow())).isNull();
	}

	@Test
	void codeTreeNodeAndHtmlContentPanelIntegration() {
		ICodeNodeRef ref = stubCodeRef();
		JadxCodeTreeNode codeNode = new JadxCodeTreeNode() {
			@Override
			public ICodeNodeRef getCodeNodeRef() {
				return ref;
			}

			@Override
			public ICodeInfo getCodeInfo() {
				return new SimpleCodeInfo("code");
			}

			@Override
			public String getName() {
				return "code-node";
			}
		};
		assertThat(codeNode.getCodeNodeRef()).isSameAs(ref);

		StubMainWindow mainWindow = new StubMainWindow();
		HtmlContentPanel panel = new HtmlContentPanel(mainWindow, codeNode, "<p>hello</p>");
		assertThat(panel.getNode()).isSameAs(codeNode);
		assertThat(panel.getHtmlArea().getText()).contains("hello");
		assertThat(panel.getHtmlArea().getFont()).isEqualTo(mainWindow.uiFont);
		assertThat(mainWindow.zoomUpdate.get()).isNotNull();

		mainWindow.uiFont = new Font(Font.SANS_SERIF, Font.PLAIN, 20);
		mainWindow.zoomUpdate.get().run();
		assertThat(panel.getHtmlArea().getFont().getSize()).isEqualTo(20);
		assertThat(codeNode.getContentPanel(mainWindow)).isNull();
	}

	private static final class StubMainWindow implements IMainWindow {
		private final AtomicReference<Runnable> zoomUpdate = new AtomicReference<>();
		private Font uiFont = new Font(Font.SANS_SERIF, Font.PLAIN, 13);

		@Override
		public IJadxSettings getSettings() {
			return new IJadxSettings() {
				@Override
				public Font getCodeFont() {
					return uiFont;
				}

				@Override
				public Font getUiFont() {
					return uiFont;
				}
			};
		}

		@Override
		public IJadxTree getTree() {
			return new IJadxTree() {
				@Override
				public void addNode(BaseTreeNodeEnum parent, ITreeNode node) {
				}

				@Override
				public void openTreeNodeContent(ITreeNode node) {
				}
			};
		}

		@Override
		public void registerZoomActions(JComponent component, Runnable update) {
			zoomUpdate.set(update);
		}
	}

	private static ICodeNodeRef stubCodeRef() {
		return new ICodeNodeRef() {
			private int defPos;

			@Override
			public ICodeAnnotation.AnnType getAnnType() {
				return ICodeAnnotation.AnnType.PKG;
			}

			@Override
			public int getDefPosition() {
				return defPos;
			}

			@Override
			public void setDefPosition(int pos) {
				this.defPos = pos;
			}
		};
	}
}
