package dk.htmlwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class SetupActivity : Activity() {

    private val prefs by lazy {
        getSharedPreferences("settings", MODE_PRIVATE)
    }

    private var localUri: Uri? = null
    private lateinit var url: EditText
    private lateinit var adBlock: CheckBox
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.extras?.getInt(
    AppWidgetManager.EXTRA_APPWIDGET_ID,
    AppWidgetManager.INVALID_APPWIDGET_ID
) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        layout.addView(TextView(this).apply {
            text = "HTML Widget"
            textSize = 26f
        })

        layout.addView(TextView(this).apply {
            text = "Indsæt adressen på en hjemmeside eller vælg en HTML-fil fra telefonen."
            textSize = 16f
        })

        url = EditText(this).apply {
            hint = "https://eksempel.dk"
            setText(
    prefs.getString(
        "url_$appWidgetId",
        ""
    )
)
            setSingleLine(true)
        }

        layout.addView(url)

        layout.addView(Button(this).apply {
            text = "Vælg HTML-fil"

            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/html"
                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    )
                }

                startActivityForResult(intent, 42)
            }
        })

        adBlock = CheckBox(this).apply {
            text = "Bloker reklamer og tracking"
            isChecked = prefs.getBoolean("adblock", true)
        }

        val refreshValues = intArrayOf(
    15, 30, 45, 60,
    120, 180, 240, 300, 360, 420,
    480, 540, 600, 660, 720
)

val savedRefreshMinutes =
    prefs.getInt("refreshMinutes_$appWidgetId", 15)

val refreshLabel = TextView(this).apply {
    textSize = 16f
}

val refreshSlider = SeekBar(this).apply {
    max = refreshValues.size - 1

    progress = refreshValues.indexOf(
        savedRefreshMinutes
    ).coerceAtLeast(0)
}

fun updateRefreshLabel() {
    val minutes = refreshValues[refreshSlider.progress]

    refreshLabel.text =
        if (minutes < 60) {
            "Opdateringsfrekvens: $minutes minutter"
        } else {
            val hours = minutes / 60
            if (hours == 1) {
                "Opdateringsfrekvens: 1 time"
            } else {
                "Opdateringsfrekvens: $hours timer"
            }
        }
}

refreshSlider.setOnSeekBarChangeListener(
    object : SeekBar.OnSeekBarChangeListener {

        override fun onProgressChanged(
            seekBar: SeekBar?,
            progress: Int,
            fromUser: Boolean
        ) {
            updateRefreshLabel()
        }

        override fun onStartTrackingTouch(
            seekBar: SeekBar?
        ) {}

        override fun onStopTrackingTouch(
            seekBar: SeekBar?
        ) {}
    }
)

updateRefreshLabel()

layout.addView(refreshLabel)
layout.addView(refreshSlider)

        layout.addView(Button(this).apply {
            text = "Gem og opdater widget"

            setOnClickListener {

                val website = url.text.toString().trim()

prefs.edit().apply {

    if (website.isNotEmpty()) {

        // Hjemmeside valgt:
        // gem URL og fjern gammel HTML-fil
        putString("url_$appWidgetId", website)
remove("localUri_$appWidgetId")

    } else if (localUri != null) {

        // HTML-fil valgt:
        // gem filen og fjern gammel hjemmeside
        putString(
    "localUri_$appWidgetId",
    localUri.toString()
)
remove("url_$appWidgetId")
    }

    putBoolean(
        "adblock",
        adBlock.isChecked
    )
putInt(
    "refreshMinutes_$appWidgetId",
    refreshValues[refreshSlider.progress]
)
    apply()
}

                val request =
                    PeriodicWorkRequestBuilder<RefreshWorker>(
                        15,
                        TimeUnit.MINUTES
                    ).build()

                WorkManager
                    .getInstance(this@SetupActivity)
                    .enqueueUniquePeriodicWork(
                        "html-widget-refresh",
                        ExistingPeriodicWorkPolicy.UPDATE,
                        request
                    )

                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {

    HtmlWidgetProvider.refresh(
        this@SetupActivity,
        intArrayOf(appWidgetId)
    )

    val resultValue = Intent().apply {
        putExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            appWidgetId
        )
    }

    setResult(
        RESULT_OK,
        resultValue
    )
    finish()
                }
                Toast.makeText(
                    this@SetupActivity,
                    "Gemt – HTML Widget er opdateret",
                    Toast.LENGTH_LONG
                ).show()
            }
        })

        setContentView(
            ScrollView(this).apply {
                addView(layout)
            }
        )
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode == 42 && resultCode == RESULT_OK) {

            data?.data?.let { uri ->

                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                localUri = uri
                url.setText("")

                Toast.makeText(
                    this,
                    "HTML-fil valgt",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
