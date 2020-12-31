package io.github.techbox.core.modules


@Module
class TestDisabledModule {
    fun onLoad(): Boolean {
        return false
    }
}