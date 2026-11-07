package com.apipocket.util

import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()
