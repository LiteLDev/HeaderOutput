package com.liteldev.headeroutput.generate

import com.liteldev.headeroutput.entity.BaseType

interface Generator {
    fun generate(type: BaseType)
}
