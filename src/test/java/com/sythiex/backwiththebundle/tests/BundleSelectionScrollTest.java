package com.sythiex.backwiththebundle.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sythiex.backwiththebundle.bundle.BundleSelection;
import com.sythiex.backwiththebundle.bundle.BundleSelectionScroll;
import org.junit.jupiter.api.Test;

class BundleSelectionScrollTest {
    @Test
    void scrollingDownStartsAtTheLastVisibleItem() {
        assertEquals(7, BundleSelectionScroll.getNextSelection(1.0, BundleSelection.NO_SELECTED_ITEM, 8));
    }

    @Test
    void scrollingUpStartsAtTheFirstVisibleItem() {
        assertEquals(0, BundleSelectionScroll.getNextSelection(-1.0, BundleSelection.NO_SELECTED_ITEM, 8));
    }

    @Test
    void selectionWrapsInBothDirections() {
        assertEquals(7, BundleSelectionScroll.getNextSelection(1.0, 0, 8));
        assertEquals(0, BundleSelectionScroll.getNextSelection(-1.0, 7, 8));
    }

    @Test
    void anEmptyVisibleRangeHasNoSelection() {
        assertEquals(
            BundleSelection.NO_SELECTED_ITEM,
            BundleSelectionScroll.getNextSelection(1.0, BundleSelection.NO_SELECTED_ITEM, 0)
        );
    }
}
