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
package fiji.plugin.trackmate.util;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.graph.TimeDirectedNeighborIndex;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.jgrapht.graph.DefaultWeightedEdge;

public class TrackNavigator {

	private final Model model;
	private final SelectionModel selectionModel;
	private final TimeDirectedNeighborIndex neighborIndex;

	public TrackNavigator(final Model model, final SelectionModel selectionModel) {
		this.model = model;
		this.selectionModel = selectionModel;
		this.neighborIndex = model.getTrackModel().getDirectedNeighborIndex();
	}

	public synchronized void nextTrack() {
		final Spot spot = getASpot();
		if (null == spot) {
			return;
		}

		final Set<Integer> trackIDs = model.getTrackModel().trackIDs(true); // if only it was navigable...
		if (trackIDs.isEmpty()) {
			return;
		}

		Integer trackID = model.getTrackModel().trackIDOf(spot);
		if (null == trackID) {
			// No track? Then move to the first one.
			trackID = model.getTrackModel().trackIDs(true).iterator().next();
		}

		final Iterator<Integer> it = trackIDs.iterator();
		Integer nextTrackID = null;
		while (it.hasNext()) {
			final Integer id = it.next();
			if (id.equals(trackID)) {
				if (it.hasNext()) {
					nextTrackID = it.next();
					break;
				}
				nextTrackID = trackIDs.iterator().next(); // loop
			}
		}

		final Set<Spot> spots = model.getTrackModel().trackSpots(nextTrackID);
		final TreeSet<Spot> ring = new TreeSet<>(Spot.frameComparator);
		ring.addAll(spots);
		Spot target = ring.ceiling(spot);
		if (null == target) {
			target = ring.floor(spot);
		}

		selectionModel.clearSelection();
		selectionModel.addSpotToSelection(target);
	}

	public synchronized void previousTrack() {
		final Spot spot = getASpot();
		if (null == spot) {
			return;
		}

		Integer trackID = model.getTrackModel().trackIDOf(spot);
		final Set<Integer> trackIDs = model.getTrackModel().trackIDs(true); // if only it was navigable...
		if (trackIDs.isEmpty()) {
			return;
		}

		Integer lastID = null;
		for (final Integer id : trackIDs) {
			lastID = id;
		}

		if (null == trackID) {
			// No track? Then take the last one.
			trackID = lastID;
		}

		final Iterator<Integer> it = trackIDs.iterator();
		Integer previousTrackID = null;
		while (it.hasNext()) {
			final Integer id = it.next();
			if (id.equals(trackID)) {
				if (previousTrackID != null) {
					break;
				}
				previousTrackID = lastID;
				break;
			}
			previousTrackID = id;
		}

		final Set<Spot> spots = model.getTrackModel().trackSpots(previousTrackID);
		final TreeSet<Spot> ring = new TreeSet<>(Spot.frameComparator);
		ring.addAll(spots);
		Spot target = ring.ceiling(spot);
		if (null == target) {
			target = ring.floor(spot);
		}

		selectionModel.clearSelection();
		selectionModel.addSpotToSelection(target);
	}

	public synchronized void nextSibling() {
		final Spot spot = getASpot();
		if (null == spot) {
			return;
		}

		final Integer trackID = model.getTrackModel().trackIDOf(spot);
		if (null == trackID) {
			return;
		}

		final int frame = spot.getFeature(Spot.FRAME).intValue();
		final TreeSet<Spot> ring = new TreeSet<>(Spot.nameComparator);

		final Set<Spot> spots = model.getTrackModel().trackSpots(trackID);
		for (final Spot s : spots) {
			final int fs = s.getFeature(Spot.FRAME).intValue();
			if (frame == fs && s != spot) {
				ring.add(s);
			}
		}

		if (!ring.isEmpty()) {
			Spot nextSibling = ring.ceiling(spot);
			if (null == nextSibling) {
				nextSibling = ring.first(); // loop
			}
			selectionModel.clearSelection();
			selectionModel.addSpotToSelection(nextSibling);
		}
	}

	public synchronized void previousSibling() {
		final Spot spot = getASpot();
		if (null == spot) {
			return;
		}

		final Integer trackID = model.getTrackModel().trackIDOf(spot);
		if (null == trackID) {
			return;
		}

		final int frame = spot.getFeature(Spot.FRAME).intValue();
		final TreeSet<Spot> ring = new TreeSet<>(Spot.nameComparator);

		final Set<Spot> spots = model.getTrackModel().trackSpots(trackID);
		for (final Spot s : spots) {
			final int fs = s.getFeature(Spot.FRAME).intValue();
			if (frame == fs && s != spot) {
				ring.add(s);
			}
		}

		if (!ring.isEmpty()) {
			Spot previousSibling = ring.floor(spot);
			if (null == previousSibling) {
				previousSibling = ring.last(); // loop
			}
			selectionModel.clearSelection();
			selectionModel.addSpotToSelection(previousSibling);
		}
	}

	public synchronized void previousInTime() {
		final Spot spot = getASpot();
		if (null == spot) {
			return;
		}

		final Set<Spot> predecessors = neighborIndex.predecessorsOf(spot);
		if (!predecessors.isEmpty()) {
			final Spot next = predecessors.iterator().next();
			selectionModel.clearSelection();
			selectionModel.addSpotToSelection(next);
		}
	}

	public synchronized void nextInTime() {
		final Spot spot = getASpot();
		if (null == spot) {
			return;
		}

		final Set<Spot> successors = neighborIndex.successorsOf(spot);
		if (!successors.isEmpty()) {
			final Spot next = successors.iterator().next();
			selectionModel.clearSelection();
			selectionModel.addSpotToSelection(next);
		}
	}

	/*
	 * STATIC METHODS
	 */

	/**
	 * Return a meaningful spot from the current selection, or <code>null</code>
	 * if the selection is empty.
	 */
	private Spot getASpot() {
		// Get it from spot selection
		final Set<Spot> spotSelection = selectionModel.getSpotSelection();
		if (!spotSelection.isEmpty()) {
			final Iterator<Spot> it = spotSelection.iterator();
			Spot spot = it.next();
			int minFrame = spot.getFeature(Spot.FRAME).intValue();
			while (it.hasNext()) {
				final Spot s = it.next();
				final int frame = s.getFeature(Spot.FRAME).intValue();
				if (frame < minFrame) {
					minFrame = frame;
					spot = s;
				}
			}
			return spot;
		}

		// Nope? Then get it from edges
		final Set<DefaultWeightedEdge> edgeSelection = selectionModel.getEdgeSelection();
		if (!edgeSelection.isEmpty()) {
			final Iterator<DefaultWeightedEdge> it = edgeSelection.iterator();
			final DefaultWeightedEdge edge = it.next();
			Spot spot = model.getTrackModel().getEdgeSource(edge);
			int minFrame = spot.getFeature(Spot.FRAME).intValue();
			while (it.hasNext()) {
				final DefaultWeightedEdge e = it.next();
				final Spot s = model.getTrackModel().getEdgeSource(e);
				final int frame = s.getFeature(Spot.FRAME).intValue();
				if (frame < minFrame) {
					minFrame = frame;
					spot = s;
				}
			}
			return spot;
		}

		// Still nothing? Then give up.
		return null;
	}
}
