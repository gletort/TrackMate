# Example TrackMate script to access the data in tables after tracking.

import os
from java.io import File
from fiji.plugin.trackmate import SelectionModel
from fiji.plugin.trackmate.io import TmXmlReader
from fiji.plugin.trackmate.visualization.table import TrackTableView

root_folder = '/Users/tinevez/Development/TrackMateWS/TrackMate/samples'

# Read data from a file.
trackmate_file_path = os.path.join(root_folder, 'FakeTracks.xml')
reader = TmXmlReader(File(trackmate_file_path))
if not reader.isReadingOk():
	print('Problem reading file %s:' % trackmate_file_path)
	print(reader.getErrorMessage())
	print('Aborting')

else:
	# Read data.
	model = reader.getModel()
	# Read display settings.
	ds = reader.getDisplaySettings()
	# Create the tables
	tables = TrackTableView( model, SelectionModel(model), ds)
	
	# Get the spot table.
	spot_table = tables.getSpotTable()
	# Export it.
	spot_export_path = os.path.join(root_folder, 'spot_table_export.csv')
	spot_table.exportToCsv(File(spot_export_path))
	print('Spot table exported to %s' % spot_export_path)
	
	# The edge table.
	edge_table = tables.getEdgeTable()
	# The track table.
	track_table = tables.getTrackTable()
	

	
	