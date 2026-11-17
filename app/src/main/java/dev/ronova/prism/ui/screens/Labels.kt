package dev.ronova.prism.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.ronova.prism.R
import dev.ronova.prism.data.Aspect

@Composable
fun aspectName(a: Aspect): String = stringResource(
    when (a) {
        Aspect.FIT -> R.string.aspect_fit
        Aspect.CROP -> R.string.aspect_crop
        Aspect.STRETCH -> R.string.aspect_stretch
        Aspect.R16_9 -> R.string.aspect_169
        Aspect.R4_3 -> R.string.aspect_43
        Aspect.ORIGINAL -> R.string.aspect_original
    }
)
