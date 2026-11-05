package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.FlareAchievements
import com.example.data.model.StreakMilestones
import com.example.data.util.SecurityUtils
import com.example.ui.theme.getStreakColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("FLARE", appName)
  }

  @Test
  fun `test 6 digit id format`() {
    val id = SecurityUtils.generateSixDigitId()
    assertEquals(6, id.length)
    assertTrue(id.all { it.isDigit() })
  }

  @Test
  fun `test password hashing`() {
    val hash = SecurityUtils.hashPassword("MUZAKY45")
    assertNotNull(hash)
    assertTrue(SecurityUtils.verifyPassword("MUZAKY45", hash))
  }

  @Test
  fun `test user achievements catalog contains at least 5 achievements`() {
    val achievements = FlareAchievements.all
    assertTrue(achievements.size >= 5)
    assertNotNull(FlareAchievements.getByKey("first_message_sent"))
    assertNotNull(FlareAchievements.getByKey("friend_finder"))
    assertNotNull(FlareAchievements.getByKey("chatterbox"))
    assertNotNull(FlareAchievements.getByKey("profile_customizer"))
    assertNotNull(FlareAchievements.getByKey("streak_starter"))
  }

  @Test
  fun `test unlimited streak milestones and dynamic color spectrum`() {
    val milestone1000 = StreakMilestones.getMilestoneForDays(1000)
    assertNotNull(milestone1000)
    assertEquals("Galactic Supernova", milestone1000?.title)

    val colorDay1 = getStreakColor(1)
    val colorDay14 = getStreakColor(14)
    val colorDay500 = getStreakColor(500)
    assertNotNull(colorDay1)
    assertNotNull(colorDay14)
    assertNotNull(colorDay500)
  }
}
