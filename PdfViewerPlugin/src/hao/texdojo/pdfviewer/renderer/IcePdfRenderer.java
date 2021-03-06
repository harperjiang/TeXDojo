package hao.texdojo.pdfviewer.renderer;

import java.awt.Frame;

import javax.swing.JPanel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.icepdf.ri.common.ComponentKeyBinding;
import org.icepdf.ri.common.SwingController;
import org.icepdf.ri.common.SwingViewBuilder;
import org.icepdf.ri.util.PropertiesManager;

public class IcePdfRenderer extends AbstractPdfRenderer {

	private SwingController controller;

	@Override
	public void init(Composite parent) {
		// Create a AWT Frame using SWT-AWT Bridge
		parent.setLayout(new FillLayout());
		Composite container = new Composite(parent, SWT.EMBEDDED);

		Frame frame = SWT_AWT.new_Frame(container);

		// build a controller
		controller = new SwingController();

		// Build a SwingViewFactory configured with the controller PropertiesManager
		PropertiesManager properties = PropertiesManager.getInstance();

		// Change the value of a couple default viewer Properties.
		// Note: this should be done before the factory is initialized.
		properties.setBoolean(PropertiesManager.PROPERTY_SHOW_TOOLBAR_ANNOTATION, false);
		properties.setBoolean(PropertiesManager.PROPERTY_SHOW_TOOLBAR_UTILITY, Boolean.FALSE);
		properties.setBoolean(PropertiesManager.PROPERTY_SHOW_TOOLBAR_FIT, Boolean.FALSE);
		SwingViewBuilder factory = new SwingViewBuilder(controller, properties);

		// Use the factory to build a JPanel that is pre-configured
		// with a complete, active Viewer UI.
		JPanel viewerComponentPanel = factory.buildViewerPanel();

		// add copy keyboard command
		ComponentKeyBinding.install(controller, viewerComponentPanel);

		// add interactive mouse link annotation support via callback
		controller.getDocumentViewController().setAnnotationCallback(
				new org.icepdf.ri.common.MyAnnotationCallback(controller.getDocumentViewController()));

		// Create a JFrame to display the panel in
		frame.add(viewerComponentPanel);
		frame.pack();

		// Open a PDF document to view
		controller.openDocument(getFile().getAbsolutePath());
	}

	@Override
	public int getNumPage() {
		return 0;
	}

	@Override
	public void showPage(int pageNum) {
		controller.showPage(pageNum);
	}

}
