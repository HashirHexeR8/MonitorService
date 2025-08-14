package com.example.monitorapp.model

data class NetworkResponseDTO<T> (
    var statusCode: Int,
    var statusMessage: String,
    var data: T?
)