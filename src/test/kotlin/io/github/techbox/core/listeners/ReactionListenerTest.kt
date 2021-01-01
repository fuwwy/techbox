package io.github.techbox.core.listeners

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageReaction
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime
import kotlin.time.seconds


@ExperimentalTime
class ReactionListenerTest : FunSpec({
    val jda = mockk<JDA>()
    val reactionListener = ReactionListener()

    fun getMember(user: User): Member {
        return mockk<Member> {
            every { idLong } returns user.idLong
        }
    }

    fun getUser(id: Long): User {
        return mockk<User> {
            every { idLong } returns id
        }
    }

    var deferred = CompletableDeferred<String>()

    val reactionOperation = ReactionListener.ReactionOperation()
    reactionOperation.onAdd {
        deferred.complete(it.reactionEmote.asReactionCode)
    }

    val reactionEmoteObj = mockk<MessageReaction.ReactionEmote> {
        every { asReactionCode } returns "✅"
        every { isEmoji } returns true
    }
    val messageReaction = mockk<MessageReaction> {
        every { reactionEmote } returns reactionEmoteObj
        every { messageIdLong } returns 96609345684406272
        every { channel } returns mockk<TextChannel> {
            every { guild } returns mockk()
        }
    }
    ReactionListener.activeOperations.put(messageReaction.messageIdLong, reactionOperation, 50, TimeUnit.SECONDS)

    val user = getUser(96609345684406272)
    val member = getMember(user)
    val badUser = getUser(400385060311793674)
    val badMember = getMember(badUser)

    test("Should successfully receive events") {
        reactionListener.onEvent(MessageReactionAddEvent(jda, 0, user, member, messageReaction, user.idLong))
        withTimeout(500) {
            deferred.await() shouldBe "✅"
        }
    }

    test("Should ignore unwhitelisted users") {
        deferred = CompletableDeferred()
        reactionOperation.allowedUsers = listOf(user)
        reactionListener.onEvent(MessageReactionAddEvent(jda, 0, badUser, badMember, messageReaction, badUser.idLong))
        withTimeout(500) {
            deferred.isCompleted shouldBe false
        }
    }

    test("Should ignore unwhitelisted reactions") {
        deferred = CompletableDeferred()
        reactionOperation.allowedReactions = listOf("❌")
        reactionListener.onEvent(MessageReactionAddEvent(jda, 0, user, member, messageReaction, user.idLong))
        withTimeout(500) {
            deferred.isCompleted shouldBe false
        }
    }

    test("Should expire") {
        ReactionListener.activeOperations.clear()
        var expired = false
        reactionOperation.onExpire {
            expired = true
        }
        ReactionListener.activeOperations.put(messageReaction.messageIdLong, reactionOperation, 1, TimeUnit.SECONDS)
        eventually(2.seconds) {
            expired shouldBe true
        }
    }

})