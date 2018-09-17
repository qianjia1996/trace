package trace.utils;
/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
import javafx.scene.control.Pagination;
import javafx.scene.control.Skin;

public class CustomPagination extends Pagination {

    public CustomPagination(int pageCount, int pageIndex) {
        super(pageCount, pageIndex);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected Skin<?> createDefaultSkin() {
        return new CustomPaginationSkin(this);
    }

}
