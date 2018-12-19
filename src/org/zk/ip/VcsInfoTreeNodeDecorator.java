package org.zk.ip;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ProjectViewNodeDecorator;
import com.intellij.ide.projectView.impl.nodes.ClassTreeNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.LocalFilePath;
import com.intellij.openapi.vcs.changes.ChangesUtil;
import com.intellij.openapi.vcs.diff.DiffMixin;
import com.intellij.openapi.vcs.history.VcsRevisionDescription;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packageDependencies.ui.PackageDependenciesNode;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ObjectUtils;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.vcsUtil.VcsUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @description:
 * @author: kun.zhu
 * @create: 2018-12-08 14:30
 **/
public final class VcsInfoTreeNodeDecorator implements ProjectViewNodeDecorator {
	private static final Logger LOG = Logger.getInstance(VcsInfoTreeNodeDecorator.class);
	private ExecutorService threadPool = Executors.newFixedThreadPool(10);

	@Override
	public void decorate(ProjectViewNode node, PresentationData data) {
		Project project = node.getProject();

		threadPool.submit(() -> {
			String cvsInfo = this.getCvsInfo(node, project);
			if (cvsInfo != null) {
				if (data.getColoredText().isEmpty() && data.getPresentableText() != null) {
					data.addText(data.getPresentableText(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
					data.addText(" <" + cvsInfo + ">", SimpleTextAttributes.GRAY_ATTRIBUTES);
				}
			}
		});
	}

	private String getCvsInfo(AbstractTreeNode node, Project project) {
		VirtualFile file = this.getVirtualFile(node);
		VcsRevisionDescription description = this.getVscDescrition(file, project);
		if (description == null) {
			return null;
		}

		String author = description.getAuthor();
		String revisionDate = DateFormatUtil.formatPrettyDateTime(description.getRevisionDate());
		String commitMessage = description.getCommitMessage();
		String revision = description.getRevisionNumber().asString();
		return author + "  " + revisionDate + "  " + revision;
	}

	private VirtualFile getVirtualFile(AbstractTreeNode node) {
		VirtualFile file = null;
		if (node instanceof ClassTreeNode) {
			file = ((ClassTreeNode) node).getValue().getContainingFile().getVirtualFile();
		} else if (node instanceof PsiFileNode) {
			file = ((PsiFileNode) node).getVirtualFile();
		}
		return file;
	}

	private VcsRevisionDescription getVscDescrition(VirtualFile file, Project project) {
		if (file == null) {
			return null;
		}

		boolean isUnderVcs = VcsUtil.getVcsFor(project, file).fileIsUnderVcs(new LocalFilePath(file.getPath(), false));
		if (!isUnderVcs) {
			return null;
		}

		AbstractVcs vcs = ChangesUtil.getVcsForFile(file, project);
		VcsRevisionDescription description = ((DiffMixin) ObjectUtils.assertNotNull((DiffMixin) vcs.getDiffProvider()))
				.getCurrentRevisionDescription(file);
		return description;
	}

	@Override
	public void decorate(PackageDependenciesNode pdNode, ColoredTreeCellRenderer renderer) {
	}

}
