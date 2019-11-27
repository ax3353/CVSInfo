package org.zk.ip;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ProjectViewNodeDecorator;
import com.intellij.ide.projectView.impl.nodes.ClassTreeNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
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

import java.util.concurrent.*;

/**
 * @author: kun.zhu
 * @create: 2018-12-08 14:30
 **/
public final class VcsInfoTreeNodeDecorator implements ProjectViewNodeDecorator {
	private ThreadFactory namedThreadFactory =
			new ThreadFactoryBuilder().setNameFormat("thread-call-runner-%d").build();
	private ExecutorService executorService = new ThreadPoolExecutor(5, 30, 0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<>(), namedThreadFactory);

	@Override
	public void decorate(ProjectViewNode node, PresentationData data) {
		Project project = node.getProject();

		executorService.submit(() -> {
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
		VcsRevisionDescription description = this.getVscDescription(file, project);
		if (description == null) {
			return null;
		}

		String author = description.getAuthor();
		String revisionDate = DateFormatUtil.formatPrettyDateTime(description.getRevisionDate());
		String revision = description.getRevisionNumber().asString().substring(0, 8);
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

	private VcsRevisionDescription getVscDescription(VirtualFile file, Project project) {
		if (file == null) {
			return null;
		}

		boolean isExistsInVcs = ObjectUtils.assertNotNull(VcsUtil.getVcsFor(project, file))
				.fileExistsInVcs(new LocalFilePath(file.getPath(), false));
		if (!isExistsInVcs) {
			return null;
		}

		AbstractVcs vcs = ChangesUtil.getVcsForFile(file, project);
		if (vcs == null) {
			return null;
		}
		return ObjectUtils.assertNotNull((DiffMixin) vcs.getDiffProvider()).getCurrentRevisionDescription(file);
	}

	@Override
	public void decorate(PackageDependenciesNode pdNode, ColoredTreeCellRenderer renderer) {
	}

}
