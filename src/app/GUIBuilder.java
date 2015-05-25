package app;

import io.CSVCountReader;
import io.GraphMLReader;
import io.Questionnaire;
import io.SvgExporter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import mip.Solver;
import model.MetroVertex;
import model.TransferGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.utils.UserData;

public class GUIBuilder {

	private JFrame frame;
	private MetroMapper app;
	private SvgCanvas svgCanvas;

	JComboBox<MetroVertex> stationComboBox;
	private ActionListener stationComboBoxActionListener;
	private MetroVertex currentSourceStation = null;

	public GUIBuilder(final MetroMapper app, JFrame frame) {

		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			UIManager.put("List.focusCellHighlightBorder", BorderFactory.createEmptyBorder());
			UIManager.put("Slider.focus", UIManager.get("Slider.background"));
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}

		this.app = app;
		this.frame = frame;

		this.stationComboBox = new JComboBox<MetroVertex>(new MetroVertex[0]);
		this.stationComboBoxActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox<MetroVertex> source = (JComboBox<MetroVertex>) e.getSource();
				Settings.sourceStation = (MetroVertex) source.getSelectedItem();
				if (Settings.sourceStation != null) {
					app.computeDijkstraWeights();
				}
			}
		};
	}

	public void build() {
		final JPanel main = new JPanel(new BorderLayout());
		final JPanel center = new JPanel(new BorderLayout());

		// Center panel (SvgCanvas)
		this.svgCanvas = new SvgCanvas(this.app);
		this.svgCanvas.setBackground(Color.white);

		// Sidebar
		JPanel sidebarGeneral = new JPanel();
		sidebarGeneral.setLayout(new FlowLayout(FlowLayout.LEADING));
		sidebarGeneral.setPreferredSize(new Dimension(300, 280));
		sidebarGeneral.setBorder(new EmptyBorder(4, 10, 10, 10));

		JPanel sidebarSchematization = new JPanel();
		sidebarSchematization.setLayout(new FlowLayout(FlowLayout.LEADING));
		sidebarSchematization.setPreferredSize(new Dimension(300, 0));
		sidebarSchematization.setBorder(new EmptyBorder(4, 10, 10, 10));

		JPanel sidebarRendering = new JPanel();
		sidebarRendering.setLayout(new FlowLayout(FlowLayout.LEADING));
		sidebarRendering.setPreferredSize(new Dimension(300, 0));
		sidebarRendering.setBorder(new EmptyBorder(4, 10, 10, 10));
		

		final JLabel bendsOnLineLabel = new JLabel("Bends on line importance:");
		final JTextField bendsOnLineInput = new JTextField();

		// "Map:" label
		JLabel mapsLabel = new JLabel("Map:");
		mapsLabel.setFont(new Font("Dialog", Font.BOLD, 11));
		sidebarGeneral.add(mapsLabel);

		// Map chooser
		JComboBox<String> mapList = new JComboBox<String>(Settings.inputMaps);
		mapList.setSelectedItem(Settings.inputMap);
		mapList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Settings.inputMap = (String) ((JComboBox) e.getSource()).getSelectedItem();
				Settings.loadedGraphMLFile = "";
				app.initializeMap();
			}
		});
		mapList.setPreferredSize(new Dimension(270, 22));
		sidebarGeneral.add(mapList);

		// Shortest paths algorithm
		JLabel pairsLabel = new JLabel("Characteristics:");
		pairsLabel.setFont(new Font("Dialog", Font.BOLD, 11));
		sidebarGeneral.add(pairsLabel);
		

		
		// "Base bends on a line importance on Dijkstra counts" checkbox
		/*JCheckBox dijkstraCountBasedBendFactors = new JCheckBox("Use modified schematization algorithm");
		dijkstraCountBasedBendFactors.setPreferredSize(new Dimension(270, 20));
		dijkstraCountBasedBendFactors.setSelected(Settings.modifiedSchematization);
		dijkstraCountBasedBendFactors.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Settings.modifiedSchematization = ((JCheckBox) e.getSource()).isSelected();
				bendsOnLineLabel.setEnabled(Settings.modifiedSchematization == false);
				bendsOnLineInput.setEnabled(Settings.modifiedSchematization == false);
			}
		});
		sidebarGeneral.add(dijkstraCountBasedBendFactors);
		*/
		
		JComboBox<String> schematizationCharacteristic = new JComboBox<String>(new String[] { "Raw", "Modified" });
		if(Settings.modifiedSchematization == true) {
			schematizationCharacteristic.setSelectedIndex(1);
		} else {
			schematizationCharacteristic.setSelectedIndex(0);
		}
		schematizationCharacteristic.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int index = ((JComboBox) e.getSource()).getSelectedIndex();
				Settings.modifiedSchematization = (index == 1);
				bendsOnLineLabel.setEnabled(Settings.modifiedSchematization == false);
				bendsOnLineInput.setEnabled(Settings.modifiedSchematization == false);
			}
		});
		schematizationCharacteristic.setPreferredSize(new Dimension(270, 22));
		sidebarGeneral.add(schematizationCharacteristic);

		JComboBox<String> visualizationCharacteristic = new JComboBox<String>(new String[] { "Source based", "All-pairs based", "Fixed source based", "Fixed" });
		if(Settings.shortestPathsAlgorithm == Settings.SHORTEST_PATHS_SINGLE_SOURCE) {
			if(Settings.fixedLineThicknesses == true) {
				visualizationCharacteristic.setSelectedIndex(2);
			} else {
				visualizationCharacteristic.setSelectedIndex(0);
			}
		} else {
			if(Settings.fixedLineThicknesses == true) {
				visualizationCharacteristic.setSelectedIndex(3);
			} else {
				visualizationCharacteristic.setSelectedIndex(1);
			}
		}
		visualizationCharacteristic.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int index = ((JComboBox) e.getSource()).getSelectedIndex();
				
				if(index == 0 || index == 2) {
					Settings.shortestPathsAlgorithm = Settings.SHORTEST_PATHS_SINGLE_SOURCE;
				} else {
					Settings.shortestPathsAlgorithm = Settings.SHORTEST_PATHS_ALL_PAIRS;
				}
							
				Settings.fixedLineThicknesses = (index == 2 || index == 3);
				
				app.repaintMap();
			}
		});
		visualizationCharacteristic.setPreferredSize(new Dimension(270, 22));
		sidebarGeneral.add(visualizationCharacteristic);
		

		// Draw arrow hints
		JComboBox<String> arrowCharacteristic = new JComboBox<String>(new String[] { "With arrow hints", "Without arrow hints" });
		if(Settings.drawArrowHints == true) {
			arrowCharacteristic.setSelectedIndex(0);
		} else {
			arrowCharacteristic.setSelectedIndex(1);
		}
		arrowCharacteristic.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int index = ((JComboBox) e.getSource()).getSelectedIndex();
				Settings.drawArrowHints = (index == 0);
				app.repaintMap();
			}
		});
		arrowCharacteristic.setPreferredSize(new Dimension(270, 22));
		sidebarGeneral.add(arrowCharacteristic);
		

		// Contract degree two vertices
		JLabel contractLabel = new JLabel("Degree-two vertices:");
		contractLabel.setFont(new Font("Dialog", Font.BOLD, 11));
		sidebarSchematization.add(contractLabel);

		JComboBox<String> contractList = new JComboBox<String>(new String[] { "Contract none", "Contract all", "Contract non-adjacent to intersection",
				"Contract non-adjacent to intersection/pendant edge" });
		contractList.setSelectedIndex(Settings.degreeTwoVerticesContractionMode);
		contractList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Settings.degreeTwoVerticesContractionMode = ((JComboBox) e.getSource()).getSelectedIndex();
			}
		});
		contractList.setPreferredSize(new Dimension(270, 22));
		sidebarSchematization.add(contractList);

		// "Planarity constraints:" label
		JLabel planarityLabel = new JLabel("Planarity constraints:");
		planarityLabel.setFont(new Font("Dialog", Font.BOLD, 11));
		sidebarSchematization.add(planarityLabel);

		JComboBox<String> planarityList = new JComboBox<String>(new String[] { "None", "All edge pairs", "Determine pairs by face heuristic", "Pendant edges only" });
		planarityList.setSelectedIndex(Settings.planarityMode);
		planarityList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Settings.planarityMode = ((JComboBox) e.getSource()).getSelectedIndex();
			}
		});
		planarityList.setPreferredSize(new Dimension(270, 22));
		sidebarSchematization.add(planarityList);

		// "Use convex hull dummy edges for faces" checkbox
		JCheckBox useConvexHullDummies = new JCheckBox("Use convex hull dummy edges for faces");
		useConvexHullDummies.setPreferredSize(new Dimension(270, 30));
		useConvexHullDummies.setSelected(Settings.convexHullDummyEdgesInFaceComputation);
		useConvexHullDummies.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Settings.convexHullDummyEdgesInFaceComputation = ((JCheckBox) e.getSource()).isSelected();
				app.repaintMap();
			}
		});
		sidebarSchematization.add(useConvexHullDummies);

		// "Minimum edge length:" label
		JLabel minimumEdgeLengthLabel = new JLabel("Minimum edge length:");
		minimumEdgeLengthLabel.setFont(new Font("Dialog", Font.BOLD, 11));
		minimumEdgeLengthLabel.setPreferredSize(new Dimension(215, 20));
		sidebarSchematization.add(minimumEdgeLengthLabel);

		final JTextField minimumEdgeLengthInput = new JTextField();
		minimumEdgeLengthInput.setPreferredSize(new Dimension(50, 20));
		minimumEdgeLengthInput.setText(String.valueOf(Settings.minimumEdgeLength));
		sidebarSchematization.add(minimumEdgeLengthInput);
		minimumEdgeLengthInput.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				warn();
			}

			public void removeUpdate(DocumentEvent e) {
				warn();
			}

			public void insertUpdate(DocumentEvent e) {
				warn();
			}

			public void warn() {
				if (minimumEdgeLengthInput.getText().length() > 0) {
					int value = (int) Integer.valueOf(minimumEdgeLengthInput.getText());
					if (value > 0 && value <= 10) {
						Settings.minimumEdgeLength = value;
					} else {
						JOptionPane.showMessageDialog(null, "Error: Please enter number between 1 and 10", "Error Massage", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});

		// "Total edge length importance:" label
		JLabel totalEdgeLengthLabel = new JLabel("Total edge length importance:");
		totalEdgeLengthLabel.setFont(new Font("Dialog", Font.BOLD, 11));
		totalEdgeLengthLabel.setPreferredSize(new Dimension(215, 20));
		sidebarSchematization.add(totalEdgeLengthLabel);

		final JTextField totalEdgeLengthInput = new JTextField();
		totalEdgeLengthInput.setPreferredSize(new Dimension(50, 20));
		totalEdgeLengthInput.setText(String.valueOf(Settings.totalEdgeLengthImportance));
		sidebarSchematization.add(totalEdgeLengthInput);
		totalEdgeLengthInput.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				warn();
			}

			public void removeUpdate(DocumentEvent e) {
				warn();
			}

			public void insertUpdate(DocumentEvent e) {
				warn();
			}

			public void warn() {
				if (totalEdgeLengthInput.getText().length() > 0) {
					int value = (int) Integer.valueOf(totalEdgeLengthInput.getText());
					if (value > 0 && value <= 10) {
						Settings.totalEdgeLengthImportance = value;
					} else {
						JOptionPane.showMessageDialog(null, "Error: Please enter number between 1 and 10", "Error Massage", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});

		// "Bends on line importance:" label
		bendsOnLineLabel.setFont(new Font("Dialog", Font.BOLD, 11));
		bendsOnLineLabel.setPreferredSize(new Dimension(215, 20));
		bendsOnLineLabel.setEnabled(Settings.modifiedSchematization == false);
		sidebarSchematization.add(bendsOnLineLabel);

		bendsOnLineInput.setPreferredSize(new Dimension(50, 20));
		bendsOnLineInput.setText(String.valueOf(Settings.bendsOnLineImportance));
		bendsOnLineInput.setEnabled(Settings.modifiedSchematization == false);
		sidebarSchematization.add(bendsOnLineInput);
		bendsOnLineInput.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				warn();
			}

			public void removeUpdate(DocumentEvent e) {
				warn();
			}

			public void insertUpdate(DocumentEvent e) {
				warn();
			}

			public void warn() {
				if (bendsOnLineInput.getText().length() > 0) {
					int value = (int) Integer.valueOf(bendsOnLineInput.getText());
					if (value > 0 && value <= 10) {
						Settings.bendsOnLineImportance = value;
					} else {
						JOptionPane.showMessageDialog(null, "Error: Please enter number between 1 and 10", "Error Massage", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});

		// "Geographic deviation importance:" label
		JLabel geographicDeviationLabel = new JLabel("Geographic deviation importance:");
		geographicDeviationLabel.setFont(new Font("Dialog", Font.BOLD, 11));
		geographicDeviationLabel.setPreferredSize(new Dimension(215, 20));
		sidebarSchematization.add(geographicDeviationLabel);

		final JTextField geographicDeviationInput = new JTextField();
		geographicDeviationInput.setPreferredSize(new Dimension(50, 20));
		geographicDeviationInput.setText(String.valueOf(Settings.geographicDeviationImportance));
		sidebarSchematization.add(geographicDeviationInput);
		geographicDeviationInput.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				warn();
			}

			public void removeUpdate(DocumentEvent e) {
				warn();
			}

			public void insertUpdate(DocumentEvent e) {
				warn();
			}

			public void warn() {
				if (geographicDeviationInput.getText().length() > 0) {
					int value = (int) Integer.valueOf(geographicDeviationInput.getText());
					if (value > 0 && value <= 10) {
						Settings.geographicDeviationImportance = value;
					} else {
						JOptionPane.showMessageDialog(null, "Error: Please enter number between 1 and 10", "Error Massage", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});

		// Solve button
		final JButton solveBtn = new JButton("Solve!");
		solveBtn.setPreferredSize(new Dimension(270, 30));
		solveBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				app.cancelSolver();
				app.setSolver(new Solver(app, solveBtn));
				app.getSolver().execute();
				solveBtn.setText("Solving...");
				solveBtn.setEnabled(false);
				frame.setTitle("MetroMapper");
			}
		});
		sidebarSchematization.add(solveBtn);

		// Stop button
		final JButton stopBtn = new JButton("Stop solving");
		stopBtn.setPreferredSize(new Dimension(270, 30));
		stopBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				app.cancelSolver();
				solveBtn.setEnabled(true);
			}
		});
		sidebarSchematization.add(stopBtn);

		// Load GraphML file button
		final JButton testBtn = new JButton("Load GraphML file..");
		testBtn.setPreferredSize(new Dimension(270, 30));
		testBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				JFileChooser chooser = new JFileChooser();
				chooser.setCurrentDirectory(new File(Settings.loadGraphMLDefaultFilePath));
				chooser.setAcceptAllFileFilterUsed(false);
				chooser.addChoosableFileFilter(new FileFilter() {

					private String getExtension(File f) {
						String ext = "";
						String s = f.getName();
						int i = s.lastIndexOf('.');

						if (i > 0 && i < s.length() - 1) {
							ext = s.substring(i + 1).toLowerCase();
						}
						return ext;
					}

					@Override
					public boolean accept(File f) {
						return f.isDirectory() || this.getExtension(f).equals("graphml");
					}

					@Override
					public String getDescription() {
						return "Graph Markup Language (*.GRAPHML)";
					}

				});
				int retrival = chooser.showOpenDialog(null);
				if (retrival == JFileChooser.APPROVE_OPTION) {
					try {

						File file = chooser.getSelectedFile();
						GraphMLReader r = new GraphMLReader();
						Settings.loadedGraphMLFile = file.getName();
						svgCanvas.renderGraph(r.loadGraphBySelectedFile(file));
						frame.setTitle(file.getName() + " - MetroMapper");

					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}

			}
		});
		sidebarSchematization.add(testBtn);

		// Export button
		final JButton export = new JButton("Export to SVG");
		export.setPreferredSize(new Dimension(270, 30));
		export.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				export.setEnabled(false);
				export.setText("Exporting...");
				SvgExporter svgExporter = new SvgExporter(svgCanvas, export);
				svgExporter.execute();
			}
		});
		sidebarSchematization.add(export);

		// Reset button
		final JButton reset = new JButton("Reset map to original vertices/edges");
		reset.setPreferredSize(new Dimension(270, 30));
		reset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int dialogResult = JOptionPane.showConfirmDialog(null, "Reset map?", "Warning", JOptionPane.YES_NO_OPTION);
				if (dialogResult == JOptionPane.YES_OPTION) {
					app.resetMap();
					System.out.println("reset");
				}
			}
		});
		sidebarSchematization.add(reset);

		/*
		 * 
		 * sidebarRendering
		 */

		final JSlider timeSlider = new JSlider(JSlider.HORIZONTAL, 0, 96, Settings.passengerCountTime);
		final String timeLabelString = "Passenger count time:";
		final JLabel timeLabel = new JLabel(timeLabelString, SwingConstants.CENTER);

		JLabel lineThicknessLabel = new JLabel("Base line thickness on:");
		lineThicknessLabel.setFont(new Font("Dialog", Font.BOLD, 11));
		sidebarRendering.add(lineThicknessLabel);

		JComboBox<String> lineThicknessList = new JComboBox<String>(new String[] { "Passenger count", "Dijkstra shortest path count" });
		lineThicknessList.setSelectedIndex(Settings.lineThicknessAlgorithm);
		lineThicknessList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Settings.lineThicknessAlgorithm = ((JComboBox) e.getSource()).getSelectedIndex();
				app.repaintMap();
				timeSlider.setEnabled(Settings.lineThicknessAlgorithm == Settings.LINE_THICKNESS_PASSENGER_COUNT);
				timeLabel.setEnabled(Settings.lineThicknessAlgorithm == Settings.LINE_THICKNESS_PASSENGER_COUNT);
			}
		});
		lineThicknessList.setPreferredSize(new Dimension(270, 22));
		sidebarRendering.add(lineThicknessList);

		JLabel drawingOptionsLabel = new JLabel("Drawing options:");
		drawingOptionsLabel.setFont(new Font("Dialog", Font.BOLD, 11));
		drawingOptionsLabel.setBorder(new EmptyBorder(10, 0, 0, 0));
		sidebarRendering.add(drawingOptionsLabel);

		// Show transfer graph
		JCheckBox showTransferGraph = new JCheckBox("Show transfer graph");
		showTransferGraph.setPreferredSize(new Dimension(270, 15));
		showTransferGraph.setSelected(Settings.renderTransferGraph);
		showTransferGraph.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Settings.renderTransferGraph = ((JCheckBox) e.getSource()).isSelected();
				ViewPosition vp = app.getSvgCanvas().getViewPositionObject();
				app.initializeMap();
				app.getSvgCanvas().setViewPosition(vp);
				app.repaintMap();
			}
		});
		sidebarRendering.add(showTransferGraph);

		// Draw vertex labels
		JCheckBox drawVertexLabels = new JCheckBox("Draw vertex labels");
		drawVertexLabels.setPreferredSize(new Dimension(270, 15));
		drawVertexLabels.setSelected(Settings.drawVertexLabels);
		drawVertexLabels.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Settings.drawVertexLabels = ((JCheckBox) e.getSource()).isSelected();
				app.repaintMap();
			}
		});
		sidebarRendering.add(drawVertexLabels);

		// Draw edge labels
		JCheckBox drawEdgeLabels = new JCheckBox("Draw edge labels");
		drawEdgeLabels.setPreferredSize(new Dimension(270, 15));
		drawEdgeLabels.setSelected(Settings.drawEdgeLabels);
		drawEdgeLabels.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Settings.drawEdgeLabels = ((JCheckBox) e.getSource()).isSelected();
				app.repaintMap();
			}
		});
		sidebarRendering.add(drawEdgeLabels);

		// Draw polygon wireframes
		JCheckBox drawPolygonWireframes = new JCheckBox("Draw polygon wireframes");
		drawPolygonWireframes.setPreferredSize(new Dimension(270, 15));
		drawPolygonWireframes.setSelected(Settings.drawPolygonWireframes);
		drawPolygonWireframes.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Settings.drawPolygonWireframes = ((JCheckBox) e.getSource()).isSelected();
				app.repaintMap();
			}
		});
		sidebarRendering.add(drawPolygonWireframes);

		// Flip map
		JCheckBox flipMap = new JCheckBox("Flip map");
		flipMap.setPreferredSize(new Dimension(270, 15));
		flipMap.setSelected(Settings.flipMap);
		flipMap.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Settings.flipMap = ((JCheckBox) e.getSource()).isSelected();
				app.initializeMap();
			}
		});
		sidebarRendering.add(flipMap);


		// "Arrow size" label
		JLabel arrowSizeLabel = new JLabel("Arrow size:");
		arrowSizeLabel.setFont(new Font("Dialog", Font.BOLD, 11));
		sidebarRendering.add(arrowSizeLabel);

		// Arrow size
		JSlider arrowSizeSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, Settings.arrowSizePercentage);
		arrowSizeSlider.setMinorTickSpacing(5);
		arrowSizeSlider.setMajorTickSpacing(20);
		arrowSizeSlider.setPaintTicks(true);
		arrowSizeSlider.setPaintLabels(true);
		arrowSizeSlider.setSnapToTicks(true);
		arrowSizeSlider.setPreferredSize(new Dimension(270, 44));
		arrowSizeSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				Settings.arrowSizePercentage = (int) source.getValue();
				app.repaintMap();
			}
		});
		sidebarRendering.add(BorderLayout.CENTER, arrowSizeSlider);

		// Station list
		JLabel sourceStationLabel = new JLabel("Source station:");
		sourceStationLabel.setFont(new Font("Dialog", Font.BOLD, 11));
		sourceStationLabel.setBorder(new EmptyBorder(10, 0, 0, 0));
		sidebarRendering.add(sourceStationLabel);

		if (Settings.sourceStation instanceof MetroVertex) {
			stationComboBox.setSelectedItem(Settings.sourceStation);
		}
		stationComboBox.addActionListener(stationComboBoxActionListener);
		stationComboBox.setPreferredSize(new Dimension(270, 22));
		stationComboBox.setRenderer(new StationListCellRenderer(stationComboBox.getRenderer()));

		sidebarRendering.add(stationComboBox);

		// "Passenger count" label
		timeLabel.setEnabled(Settings.lineThicknessAlgorithm == Settings.LINE_THICKNESS_PASSENGER_COUNT);
		timeLabel.setFont(new Font("Dialog", Font.BOLD, 11));
		sidebarRendering.add(BorderLayout.NORTH, timeLabel);

		// Passenger count slider
		timeSlider.setEnabled(Settings.lineThicknessAlgorithm == Settings.LINE_THICKNESS_PASSENGER_COUNT);
		timeSlider.setMinorTickSpacing(1);
		timeSlider.setMajorTickSpacing(24);
		timeSlider.setPaintTicks(true);
		timeSlider.setPaintLabels(true);
		timeSlider.setSnapToTicks(true);
		timeSlider.setPreferredSize(new Dimension(270, 44));

		Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		final String[] mapTimes = CSVCountReader.mapTimes();

		String times = mapTimes[Settings.passengerCountTime % 96];
		String label = times.substring(0, 2) + ":" + times.substring(2, 4);
		timeLabel.setText(timeLabelString + " (" + label + ")");

		for (int i = 0; i <= mapTimes.length; i++) {
			if (i % timeSlider.getMajorTickSpacing() == 0) {
				String times2 = mapTimes[i % 96];
				String label2 = times2.substring(0, 2) + ":" + times2.substring(2, 4);
				labels.put(i, new JLabel(label2));
			}
		}
		timeSlider.setLabelTable(labels);

		timeSlider.addChangeListener(new ChangeListener() {

			private long lastRepaint = 0;
			private int repaintThrottle = 500; // minimal time between two repaints

			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				Settings.passengerCountTime = source.getValue() % 96;

				String times = mapTimes[Settings.passengerCountTime % 96];
				String label = times.substring(0, 2) + ":" + times.substring(2, 4);
				timeLabel.setText(timeLabelString + " (" + label + ")");

				long t = System.currentTimeMillis();
				if (t > lastRepaint + repaintThrottle) {
					lastRepaint = t;
					svgCanvas.repaint();
				}
			}
		});
		sidebarRendering.add(BorderLayout.CENTER, timeSlider);

		// "Total edge length importance:" label
		JLabel transferTimeLabel = new JLabel("Transfer time when changing trains:");
		transferTimeLabel.setFont(new Font("Dialog", Font.BOLD, 11));
		transferTimeLabel.setPreferredSize(new Dimension(215, 20));
		sidebarGeneral.add(transferTimeLabel);

		final JTextField transferTimeInput = new JTextField();
		transferTimeInput.setPreferredSize(new Dimension(50, 20));
		transferTimeInput.setText(String.valueOf(Settings.defaultTransferTimeInSeconds));
		sidebarGeneral.add(transferTimeInput);
		transferTimeInput.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				warn();
			}

			public void removeUpdate(DocumentEvent e) {
				warn();
			}

			public void insertUpdate(DocumentEvent e) {
				warn();
			}

			public void warn() {
				if (transferTimeInput.getText().length() > 0) {
					int value = (int) Integer.valueOf(transferTimeInput.getText());
					if (value >= 0 && value <= 10000) {
						Settings.defaultTransferTimeInSeconds = value;
						ViewPosition vp = app.getSvgCanvas().getViewPositionObject();
						saveCurrentSourceStationSelection();
						app.initializeMap();
						app.getSvgCanvas().setViewPosition(vp);
						loadCurrentSourceStationSelection();
						app.repaintMap();
					} else {
						JOptionPane.showMessageDialog(null, "Error: Please enter number between 1 and 10000", "Error Massage", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});

		// Generate Question button
		final JButton questionBtn = new JButton("Generate question");
		questionBtn.setPreferredSize(new Dimension(270, 30));
		questionBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(Settings.sourceStation != null && Settings.destinationStation != null) {
					Settings.isGeneratingQuestion = true;
					app.repaintMap();
					String path = "";
					try {
						path = Questionnaire.generateQuestion(app);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					JOptionPane.showMessageDialog(frame, "Wrote to " + path + "!", "Success!", JOptionPane.INFORMATION_MESSAGE);
					Settings.isGeneratingQuestion = false;
				} else {
					JOptionPane.showMessageDialog(frame, "Specify source and destination stations first!", "Warning!", JOptionPane.WARNING_MESSAGE);
				}
			}
		});
		sidebarGeneral.add(questionBtn);
		
		
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Schematization", null, sidebarSchematization, null);
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);
		tabbedPane.addTab("Rendering", null, sidebarRendering, null);
		tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);
		tabbedPane.setSelectedIndex(0); // Default tab
		
		JTabbedPane tabbedQuestionnairePane = new JTabbedPane();
		tabbedQuestionnairePane.addTab("General / Questionnaire", null, sidebarGeneral, null);
		tabbedQuestionnairePane.setSelectedIndex(0); // Default tab
		
		JPanel splitPane = new JPanel();
		splitPane.setLayout(new BorderLayout());
		splitPane.add(BorderLayout.NORTH, tabbedQuestionnairePane);
		splitPane.add(BorderLayout.CENTER, tabbedPane);
		
		center.add(BorderLayout.CENTER, this.svgCanvas);
		main.add(BorderLayout.CENTER, center);
		main.add(BorderLayout.WEST, splitPane);

		this.frame.getContentPane().add(main);
		this.frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		this.frame.setSize(1400, 900);
		this.frame.setLocationRelativeTo(null);
		this.frame.setVisible(true);
	}

	public void loadCurrentSourceStationSelection() {
		if(currentSourceStation instanceof MetroVertex) {
			JComboBox box = app.getGUIBuilder().stationComboBox;
			for (int i = 0; i < box.getItemCount(); i++) {
				MetroVertex v = (MetroVertex) box.getItemAt(i);
				if (v.getName().equals(currentSourceStation.getName())) {
					box.setSelectedIndex(i);
					break;
				}
			}
		}
	}

	public void saveCurrentSourceStationSelection() {
		JComboBox box = app.getGUIBuilder().stationComboBox;
		currentSourceStation = (MetroVertex) box.getSelectedItem();
	}

	public SvgCanvas getSvgCanvas() {
		return this.svgCanvas;
	}

	public void setGraph(Graph g) {
		saveCurrentSourceStationSelection();
		this.stationComboBox.removeAllItems();

		ArrayList<MetroVertex> list = new ArrayList<MetroVertex>();
		Iterator it = g.getVertices().iterator();
		while (it.hasNext()) {
			list.add((MetroVertex) it.next());
		}

		Collections.sort(list, new Comparator<MetroVertex>() {
			public int compare(MetroVertex v1, MetroVertex v2) {
				return v1.getName().compareToIgnoreCase(v2.getName());
			}
		});

		this.stationComboBox.removeActionListener(this.stationComboBoxActionListener);
		for (MetroVertex v : list) {
			this.stationComboBox.addItem(v);
		}
		this.stationComboBox.addActionListener(this.stationComboBoxActionListener);
		loadCurrentSourceStationSelection();
	}

}

final class StationListCellRenderer implements ListCellRenderer {
	private final ListCellRenderer originalRenderer;

	public StationListCellRenderer(final ListCellRenderer originalRenderer) {
		this.originalRenderer = originalRenderer;
	}

	public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
		String str = "";
		if (value instanceof MetroVertex) {
			MetroVertex v = (MetroVertex) value;
			str = v.getName();
		}
		return originalRenderer.getListCellRendererComponent(list, str, index, isSelected, cellHasFocus);
	}

}

class StationListRenderer extends BasicComboBoxRenderer {

	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

		if (value instanceof MetroVertex) {
			MetroVertex v = (MetroVertex) value;
			setText(v.getName());
		}

		return this;
	}
}