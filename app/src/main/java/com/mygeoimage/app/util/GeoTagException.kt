package com.mygeoimage.app.util


class GeoTagException : Exception {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)

    private var errorCode = 0


    constructor(message: String?, errorCode: Int) : super(message) {
        this.errorCode = errorCode
    }
}