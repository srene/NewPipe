package io.fluentic.ubicdn.fragments.list;

import io.fluentic.ubicdn.fragments.ViewContract;

public interface ListViewContract<I, N> extends ViewContract<I> {
    void showListFooter(boolean show);

    void handleNextItems(N result);
}
