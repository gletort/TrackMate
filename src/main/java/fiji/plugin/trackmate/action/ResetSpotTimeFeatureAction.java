/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2023 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
/**
 *
 */
package fiji.plugin.trackmate.action;

import static fiji.plugin.trackmate.gui.Icons.TIME_ICON;

import java.awt.Frame;
import java.util.Iterator;
import java.util.Set;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;

public class ResetSpotTimeFeatureAction extends AbstractTMAction {


	public static final String NAME = "Reset spot time";
	public static final String INFO_TEXT = "<html>" +
			"Reset the time feature of all spots: it is set to the frame number "  +
			"times the frame interval. " +
			"</html>";

	private static final String KEY = "RESET_SPOT_TIME";


	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
	{
		logger.log("Reset spot time.\n");
		double dt = trackmate.getSettings().dt;
		if (dt == 0) {
			dt = 1;
		}
		final SpotCollection spots = trackmate.getModel().getSpots();
		final Set<Integer> frames = spots.keySet();
		for(final int frame : frames) {
			for (final Iterator<Spot> iterator = spots.iterator(frame, true); iterator.hasNext();) {
				iterator.next().putFeature(Spot.POSITION_T, frame * dt);
			}
			logger.setProgress((double) (frame + 1) / frames.size());
		}
		logger.log("Done.\n");
		logger.setProgress(0);
	}

	@Plugin( type = TrackMateActionFactory.class, visible = false )
	public static class Factory implements TrackMateActionFactory
	{

		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public String getName()
		{
			return NAME;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public ImageIcon getIcon()
		{
			return TIME_ICON;
		}

		@Override
		public TrackMateAction create()
		{
			return new ResetSpotTimeFeatureAction();
		}
	}
}
