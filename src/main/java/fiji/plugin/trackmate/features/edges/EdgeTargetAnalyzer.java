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
package fiji.plugin.trackmate.features.edges;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;

@Plugin( type = EdgeAnalyzer.class )
public class EdgeTargetAnalyzer extends AbstractEdgeAnalyzer
{

	public static final String KEY = "Edge target";

	public static final String SPOT_SOURCE_ID = "SPOT_SOURCE_ID";
	public static final String SPOT_TARGET_ID = "SPOT_TARGET_ID";
	public static final String EDGE_COST = "LINK_COST";
	public static final List< String > FEATURES = new ArrayList<>( 3 );
	public static final Map< String, String > FEATURE_NAMES = new HashMap<>( 3 );
	public static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap<>( 3 );
	public static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap<>( 3 );
	public static final Map< String, Boolean > IS_INT = new HashMap<>( 3 );

	static
	{
		FEATURES.add( SPOT_SOURCE_ID );
		FEATURES.add( SPOT_TARGET_ID );
		FEATURES.add( EDGE_COST );

		FEATURE_NAMES.put( SPOT_SOURCE_ID, "Source spot ID" );
		FEATURE_NAMES.put( SPOT_TARGET_ID, "Target spot ID" );
		FEATURE_NAMES.put( EDGE_COST, "Edge cost" );

		FEATURE_SHORT_NAMES.put( SPOT_SOURCE_ID, "Source ID" );
		FEATURE_SHORT_NAMES.put( SPOT_TARGET_ID, "Target ID" );
		FEATURE_SHORT_NAMES.put( EDGE_COST, "Cost" );

		FEATURE_DIMENSIONS.put( SPOT_SOURCE_ID, Dimension.NONE );
		FEATURE_DIMENSIONS.put( SPOT_TARGET_ID, Dimension.NONE );
		FEATURE_DIMENSIONS.put( EDGE_COST, Dimension.COST );

		IS_INT.put( SPOT_SOURCE_ID, Boolean.TRUE );
		IS_INT.put( SPOT_TARGET_ID, Boolean.TRUE );
		IS_INT.put( EDGE_COST, Boolean.FALSE );
	}

	public EdgeTargetAnalyzer()
	{
		super( KEY, KEY, FEATURES, FEATURE_NAMES, FEATURE_SHORT_NAMES, FEATURE_DIMENSIONS, IS_INT );
	}

	@Override
	protected void analyze( final DefaultWeightedEdge edge, final Model model )
	{
		final FeatureModel featureModel = model.getFeatureModel();
		// Edge weight
		featureModel.putEdgeFeature( edge, EDGE_COST, model.getTrackModel().getEdgeWeight( edge ) );
		// Source & target name & ID
		final Spot source = model.getTrackModel().getEdgeSource( edge );
		featureModel.putEdgeFeature( edge, SPOT_SOURCE_ID, Double.valueOf( source.ID() ) );
		final Spot target = model.getTrackModel().getEdgeTarget( edge );
		featureModel.putEdgeFeature( edge, SPOT_TARGET_ID, Double.valueOf( target.ID() ) );
	}
}
