package io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.SwingWorker;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import app.Settings;
import app.SvgCanvas;

public class SvgExporter extends SwingWorker<Void, Void> {

	private SvgCanvas svgCanvas;
	private JButton exportButton;

	public SvgExporter(SvgCanvas svgCanvas, JButton exportButton) {
		this.svgCanvas = svgCanvas;
		this.exportButton = exportButton;
	}

	public void done() {
		this.exportButton.setText("Export to SVG");
		this.exportButton.setEnabled(true);
	}

	@Override
	protected Void doInBackground() throws Exception {
		DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
		String svgNS = "http://www.w3.org/2000/svg";
		Document document = domImpl.createDocument(svgNS, "svg", null);
		SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
		svgCanvas.paint(svgGenerator);

		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File(Settings.saveSVGDefaultFilePath));
		int retrival = chooser.showSaveDialog(null);
		if (retrival == JFileChooser.APPROVE_OPTION) {
			try {
				File file = chooser.getSelectedFile();
				if (file.toString().endsWith(".svg") == false) {
					file = new File(file.getPath() + ".svg");
				}

				FileOutputStream fop = new FileOutputStream(file);
				Writer out = new OutputStreamWriter(fop, "UTF-8");
				if (!file.exists()) {
					file.createNewFile();
				}

				svgGenerator.stream(out, true);

				out.flush();
				out.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		return null;
	}

}
