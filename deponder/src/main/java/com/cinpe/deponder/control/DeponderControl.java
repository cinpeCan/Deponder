package com.cinpe.deponder.control;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;

/**
 * @Description: 描述
 * @Author: Cinpe
 * @E-Mail: cinpeCan@outlook.com
 * @CreateDate: 2021/12/22
 * @Version: 0.01
 */
public interface DeponderControl<PO, RO> {

    void submit(@NonNull Collection<View> pList);

    void submit(@NonNull Collection<View> pList, @Nullable Collection<View> rList);

    void submit(@NonNull Collection<View> pList, @Nullable Collection<View> rList, float scale);

}
