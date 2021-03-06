package hao.texdojo.latexeditor.editors.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import hao.texdojo.latexeditor.editors.LaTeXEditor;

public abstract class TextHandler extends AbstractHandler {

	protected LaTeXEditor editor;
	
	protected abstract Object executeWithEditor(ExecutionEvent event) throws ExecutionException;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		IEditorPart editor = window.getActivePage().getActiveEditor();

		if (editor instanceof LaTeXEditor) {
			this.editor = (LaTeXEditor) editor;
			return executeWithEditor(event);
		} else {
			MessageDialog.openWarning(window.getShell(), "No LaTeX Editor Found",
					"No LaTeX Editor Found for the command. This is a bug. Please contact author.");
			return null;
		}
	}

}
