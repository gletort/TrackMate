package fiji.plugin.trackmate.gui;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;

import java.awt.FileDialog;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jdom.DataConversionException;
import org.jdom.JDOMException;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.FeatureThreshold;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModelInterface;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.gui.TrackMateFrameController.GuiState;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;
import fiji.plugin.trackmate.tracking.TrackerSettings;
import fiji.plugin.trackmate.visualization.SpotDisplayer;
import fiji.plugin.trackmate.visualization.TrackMateModelManager;
import fiji.plugin.trackmate.visualization.SpotDisplayer.DisplayerType;

/**
 * This class is in charge of reading a whole TrackMate file, and return a  
 * {@link TrackMateModelInterface} with its field set. Optionally, 
 * it can also position correctly the state of the GUI.
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Apr 28, 2011
 */
public class GuiReader {

	private TrackMateFrameController controller;
	private Logger logger = Logger.VOID_LOGGER;
	
	/*
	 * CONSTRUCTORS
	 */
	
	/**
	 * Construct a {@link GuiReader} with a target file (can be null) and no {@link TrackMateFrameController} to modify.
	 */
	public GuiReader() {
		this(null);
	}
	
	/**
	 * Construct a {@link GuiReader}. The {@link TrackMateFrameController} will have its state
	 * set according to the data found in the file read.
	 * @param controller
	 */
	public GuiReader(TrackMateFrameController controller) {
		this.controller = controller;
		if (null != controller)
			logger = controller.view.getLogger();
	}
	
	
	/*
	 * METHODS
	 */
	
	
	public TrackMateModelInterface loadFile(File file) {
		
		TrackMateFrame view;
		if (null == controller) 
			view = null;
		else
			view = controller.view;
		
		TrackMateModelInterface model = new TrackMate_();
		logger.log("Opening file "+file.getName()+'\n');
		TmXmlReader reader = new TmXmlReader(file);
		try {
			reader.parse();
		} catch (JDOMException e) {
			logger.error("Problem parsing "+file.getName()+", it is not a valid TrackMate XML file.\nError message is:\n"
					+e.getLocalizedMessage()+'\n');
		} catch (IOException e) {
			logger.error("Problem reading "+file.getName()
					+".\nError message is:\n"+e.getLocalizedMessage()+'\n');
		}
		logger.log("  Parsing file done.\n");
		
		Settings settings = null;
		ImagePlus imp = null;
		
		{ // Read settings
			try {
				settings = reader.getSettings();
			} catch (DataConversionException e) {
				logger.error("Problem reading the settings field of "+file.getName()
						+". Error message is:\n"+e.getLocalizedMessage()+'\n');
				return null;
			}
			logger.log("  Reading settings done.\n");

			// Try to read image
			imp = reader.getImage();		
			if (null == imp) {
				// Provide a dummy empty image if linked image can't be found
				logger.log("Could not find image "+settings.imageFileName+" in "+settings.imageFolder+". Substituting dummy image.\n");
				imp = NewImage.createByteImage("Empty", settings.width, settings.height, settings.nframes * settings.nslices, NewImage.FILL_BLACK);
				imp.setDimensions(1, settings.nslices, settings.nframes);
			}

			settings.imp = imp;
			model.setSettings(settings);
			logger.log("  Reading image done.\n");
			// We display it only if we have a GUI
		}


		{ // Try to read segmenter settings
			SegmenterSettings segmenterSettings = null;
			try {
				segmenterSettings = reader.getSegmenterSettings();
			} catch (DataConversionException e1) {
				logger.error("Problem reading the segmenter settings field of "+file.getName()
						+". Error message is:\n"+e1.getLocalizedMessage()+'\n');
			}
			if (null == segmenterSettings) {
				// Fill in defaults
				segmenterSettings = new SegmenterSettings();
				settings.segmenterSettings = segmenterSettings;
				settings.segmenterType = segmenterSettings.segmenterType;
				settings.trackerSettings = new TrackerSettings();
				settings.trackerType = settings.trackerSettings.trackerType;
				model.setSettings(settings);
				if (null != controller) {
					controller.model = model;
					view.setModel(model);
					// Stop at start panel
					controller.state = GuiState.START;
					if (!imp.isVisible())
						imp.show();
				}
				logger.log("Loading data finished.\n");
				return model;
			}

			settings.segmenterSettings = segmenterSettings;
			settings.segmenterType = segmenterSettings.segmenterType;
			settings.trackerSettings = new TrackerSettings(); // put defaults for now
			settings.trackerType = settings.trackerSettings.trackerType;
			model.setSettings(settings);
			logger.log("  Reading segmenter settings done.\n");
		}
		
		
		{ // Try to read spots
			SpotCollection spots = null;
			try {
				spots = reader.getAllSpots();
			} catch (DataConversionException e) {
				logger.error("Problem reading the spots field of "+file.getName()
						+". Error message is\n"+e.getLocalizedMessage()+'\n');
			}
			if (null == spots) {
				// No spots, so we stop here, and switch to the segmenter panel
				if (null != controller) {
					controller.model = model;
					controller.state = GuiState.TUNE_SEGMENTER;
					view.setModel(model);
					if (!imp.isVisible())
						imp.show();
				}
				logger.log("Loading data finished.\n");
				return model;
			}
			
			// We have a spot field, update the model.
			model.setSpots(spots);
			logger.log("  Reading spots done.\n");
		}
		
		
		{ // Try to read the initial threshold
			FeatureThreshold initialThreshold = null;
			try {
				initialThreshold = reader.getInitialThreshold();
			} catch (DataConversionException e) {
				logger.error("Problem reading the initial threshold field of "+file.getName()
						+". Error message is\n"+e.getLocalizedMessage()+'\n');
			}

			if (initialThreshold == null) {
				// No initial threshold, so set it
				if (null != controller) {
					controller.model = model;
					view.setModel(model);
					controller.state = GuiState.INITIAL_THRESHOLDING;
					if (!imp.isVisible())
						imp.show();
				}
				logger.log("Loading data finished.\n");
				return model;
			}

			// Store it in model
			model.setInitialThreshold(initialThreshold.value);
			logger.log("  Reading initial threshold done.\n");
		}		
		
		{ // Try to read feature thresholds
			List<FeatureThreshold> featureThresholds = null;
			try {
				featureThresholds = reader.getFeatureThresholds();
			} catch (DataConversionException e) {
				logger.error("Problem reading the feature threholds field of "+file.getName()
						+". Error message is\n"+e.getLocalizedMessage()+'\n');
			}

			if (null == featureThresholds) {
				// No feature thresholds, we assume we have the features calculated, and put ourselves
				// in a state such that the threshold GUI will be displayed.
				if (null != controller) {
					controller.manager = new TrackMateModelManager(model);
					view.setModel(model);
					controller.state = GuiState.CALCULATE_FEATURES;
					controller.actionFlag = true;
					controller.displayer = SpotDisplayer.instantiateDisplayer(DisplayerType.HYPERSTACK_DISPLAYER, model);	
					controller.displayer.addSpotCollectionEditListener(controller.manager);	
					if (!imp.isVisible())
						imp.show();
				}
				logger.log("Loading data finished.\n");
				return model;
			}

			// Store thresholds in model
			model.setFeatureThresholds(featureThresholds);
			logger.log("  Reading feature thresholds done.\n");
		}


		{ // Try to read spot selection
			SpotCollection selectedSpots = null;
			try {
				selectedSpots = reader.getSpotSelection(model.getSpots());
			} catch (DataConversionException e) {
				logger.error("Problem reading the spot selection field of "+file.getName()+". Error message is\n"+e.getLocalizedMessage()+'\n');
			}

			// No spot selection, so we display the feature threshold GUI, with the loaded feature threshold
			// already in place.
			if (null == selectedSpots) {
				if (null != controller) {
					controller.manager = new TrackMateModelManager(model);
					view.setModel(model);
					controller.state = GuiState.CALCULATE_FEATURES;
					controller.actionFlag = true;
					controller.displayer = SpotDisplayer.instantiateDisplayer(DisplayerType.HYPERSTACK_DISPLAYER, model);
					controller.displayer.setSpots(model.getSpots());
					controller.displayer.addSpotCollectionEditListener(controller.manager);
					if (!imp.isVisible())
						imp.show();
				}
				logger.log("Loading data finished.\n");
				return model;
			}

			model.setSpotSelection(selectedSpots);
			logger.log("  Reading spot selection done.\n");
		}
		

		{ // Try to read tracker settings
			TrackerSettings trackerSettings = null;
			try {
				trackerSettings = reader.getTrackerSettings();
			} catch (DataConversionException e) {
				logger.error("Problem reading the tracker settings field of "+file.getName()
						+". Error message is:\n"+e.getLocalizedMessage()+'\n');
			}
			if (null == trackerSettings) {
				// Fill in defaults
				trackerSettings = new TrackerSettings();
				settings.trackerSettings = trackerSettings;
				settings.trackerType = trackerSettings.trackerType;
				model.setSettings(settings);
				if (null != controller) {
					controller.manager = new TrackMateModelManager(model);
					view.setModel(model);
					// Stop at tune tracker panel
					controller.state = GuiState.TUNE_TRACKER;
					controller.displayer = SpotDisplayer.instantiateDisplayer(DisplayerType.HYPERSTACK_DISPLAYER, model);
					controller.displayer.setSpots(model.getSpots());
					controller.displayer.setSpotsToShow(model.getSelectedSpots());
					controller.displayer.addSpotCollectionEditListener(controller.manager);
					if (!imp.isVisible())
						imp.show();
				}
				logger.log("Loading data finished.\n");
				return model;
			}

			settings.trackerSettings = trackerSettings;
			settings.trackerType = trackerSettings.trackerType;
			model.setSettings(settings);
			logger.log("  Reading tracker settings done.\n");
		}
		

		{ // Try reading the tracks 
			SimpleWeightedGraph<Spot, DefaultWeightedEdge> trackGraph = null; 
			try {
				trackGraph = reader.getTracks(model.getSelectedSpots());
			} catch (DataConversionException e) {
				logger.error("Problem reading the track field of "+file.getName()
						+". Error message is\n"+e.getLocalizedMessage()+'\n');
			}
			if (null == trackGraph) {
				if (null != controller) {
					controller.manager = new TrackMateModelManager(model);
					view.setModel(model);
					// Stop at tune tracker panel
					controller.state = GuiState.TUNE_TRACKER;
					controller.displayer = SpotDisplayer.instantiateDisplayer(DisplayerType.HYPERSTACK_DISPLAYER, model);
					controller.displayer.setSpots(model.getSpots());
					controller.displayer.setSpotsToShow(model.getSelectedSpots());
					controller.displayer.addSpotCollectionEditListener(controller.manager);
					if (!imp.isVisible())
						imp.show();
				}
				logger.log("Loading data finished.\n");
				return model;
			}
			
			logger.log("  Reading tracks done.\n");
			model.setTrackGraph(trackGraph);
		}
		
		controller.manager = new TrackMateModelManager(model);
		view.setModel(model);
		controller.state = GuiState.TRACKING;
		controller.actionFlag = true; // force redraw and relinking
		controller.displayer = SpotDisplayer.instantiateDisplayer(DisplayerType.HYPERSTACK_DISPLAYER, model);
		controller.displayer.setSpots(model.getSpots());
		controller.displayer.setSpotsToShow(model.getSelectedSpots());
		controller.displayer.setTrackGraph(model.getTrackGraph());
		controller.displayer.addSpotCollectionEditListener(controller.manager);
		imp.show();
		logger.log("Loading data finished.\n");
		return model;
	}
	
	
	public File askForFile(File file) {
		JFrame parent;
		if (null == controller) 
			parent = null;
		else
			parent = controller.view;
		
		if(IJ.isMacintosh()) {
			// use the native file dialog on the mac
			FileDialog dialog =	new FileDialog(parent, "Select a TrackMate file", FileDialog.LOAD);
			dialog.setDirectory(file.getParent());
			dialog.setFile(file.getName());
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".xml");
				}
			};
			dialog.setFilenameFilter(filter);
			dialog.setVisible(true);
			String selectedFile = dialog.getFile();
			if (null == selectedFile) {
				logger.log("Load data aborted.\n");
				return null;
			}
			file = new File(dialog.getDirectory(), selectedFile);
			
		} else {
			// use a swing file dialog on the other platforms
			JFileChooser fileChooser = new JFileChooser(file.getParent());
			fileChooser.setSelectedFile(file);
			FileNameExtensionFilter filter = new FileNameExtensionFilter("XML files", "xml");
			fileChooser.setFileFilter(filter);
			int returnVal = fileChooser.showOpenDialog(parent);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				file = fileChooser.getSelectedFile();
			} else {
				logger.log("Load data aborted.\n");
				return null;  	    		
			}
		}
		return file;
	}
	
	
}
