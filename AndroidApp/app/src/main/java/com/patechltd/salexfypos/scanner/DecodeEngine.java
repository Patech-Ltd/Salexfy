package com.patechltd.salexfypos.scanner;

import androidx.camera.core.ImageProxy;

public interface DecodeEngine {
    DecodeResult decode(ImageProxy image);
}
