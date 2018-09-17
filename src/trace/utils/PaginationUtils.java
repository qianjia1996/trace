package trace.utils;

import javafx.geometry.HPos;
import javafx.geometry.VPos;

public class PaginationUtils {
    static double computeXOffset(double width, double contentWidth, HPos hpos) {
        if (hpos == null) {
            return 0;
        }

        switch (hpos) {
            case LEFT:
                return 0;
            case CENTER:
                return (width - contentWidth) / 2;
            case RIGHT:
                return width - contentWidth;
            default:
                return 0;
        }
    }

    static double computeYOffset(double height, double contentHeight, VPos vpos) {
        if (vpos == null) {
            return 0;
        }

        switch (vpos) {
            case TOP:
                return 0;
            case CENTER:
                return (height - contentHeight) / 2;
            case BOTTOM:
                return height - contentHeight;
            default:
                return 0;
        }
    }
}
