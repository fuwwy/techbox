package io.github.techbox.core.modules

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.endWith
import java.lang.IllegalArgumentException


class ModuleRegistryTest: FunSpec({
    test("Modules should register successfully") {
        val moduleRegistry = ModuleRegistry()
        val module = Class.forName("io.github.techbox.core.modules.TestModule")
        moduleRegistry.register(module)
        val registeredModule = moduleRegistry.modules.entries.firstOrNull()
        registeredModule?.key shouldBe "Test"
        registeredModule?.value?.clazz shouldBe module
        registeredModule?.value?.enabled shouldBe true
    }

    test("Modules should disabled when onLoad returns false") {
        val moduleRegistry = ModuleRegistry()
        val module = Class.forName("io.github.techbox.core.modules.TestDisabledModule")
        moduleRegistry.register(module)
        val registeredModule = moduleRegistry.modules.entries.firstOrNull()
        registeredModule?.key shouldBe "TestDisabled"
        registeredModule?.value?.clazz shouldBe module
        registeredModule?.value?.enabled shouldBe false
    }

    test("Registering should fail when annotation is not present") {
        val moduleRegistry = ModuleRegistry()
        val module = Class.forName("io.github.techbox.core.modules.TestInvalidModule")
        val exception = shouldThrow<IllegalArgumentException> {
            moduleRegistry.register(module)
        }
        exception.message should endWith("is not a valid module.")
    }
})