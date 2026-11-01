package dk.htmlwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
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
        getSharedPreferences(
            "settings",
            MODE_PRIVATE
        )
    }

    private var localUri: Uri? = null

    private lateinit var url: EditText
    private lateinit var adBlock: CheckBox
    private lateinit var hideCookieBanners: CheckBox
    private lateinit var showUpdateStatus: CheckBox

    private var appWidgetId =
        AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        appWidgetId =
            intent?.extras?.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val layout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    40,
                    40,
                    40,
                    40
                )
            }

        /*
         * TITEL
         */
        layout.addView(
            TextView(this).apply {

                text = "HTML Widget"
                textSize = 26f
            }
        )

        layout.addView(
            TextView(this).apply {

                text =
                    "Indsæt adressen på en hjemmeside eller vælg en HTML-fil fra telefonen."

                textSize = 16f
            }
        )

        /*
         * WEBSITE
         */
        url =
            EditText(this).apply {

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

        /*
         * VÆLG OMRÅDE
         */
        layout.addView(
            Button(this).apply {

                text =
                    "Vælg område på websiden"

                setOnClickListener {

                    val website =
                        url.text
                            .toString()
                            .trim()

                    if (website.isEmpty()) {

                        Toast.makeText(
                            this@SetupActivity,
                            "Indtast først en hjemmesideadresse",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@setOnClickListener
                    }

                    /*
                     * Gem URL inden crop-vinduet åbnes.
                     */
                    prefs.edit()
                        .putString(
                            "url_$appWidgetId",
                            website
                        )
                        .apply()

                    val cropIntent =
                        Intent(
                            this@SetupActivity,
                            CropWebsiteActivity::class.java
                        ).apply {

                            putExtra(
                                AppWidgetManager.EXTRA_APPWIDGET_ID,
                                appWidgetId
                            )

                            putExtra(
                                "website",
                                website
                            )
                        }

                    startActivity(cropIntent)
                }
            }
        )

        /*
         * LOKAL HTML-FIL
         */
        layout.addView(
            Button(this).apply {

                text =
                    "Vælg HTML-fil"

                setOnClickListener {

                    val fileIntent =
                        Intent(
                            Intent.ACTION_OPEN_DOCUMENT
                        ).apply {

                            addCategory(
                                Intent.CATEGORY_OPENABLE
                            )

                            type =
                                "text/html"

                            addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                            )
                        }

                    startActivityForResult(
                        fileIntent,
                        42
                    )
                }
            }
        )

        /*
         * REKLAMEBLOKERING
         */
        adBlock =
            CheckBox(this).apply {

                text =
                    "Bloker reklamer og tracking"

                isChecked =
                    prefs.getBoolean(
                        "adblock",
                        true
                    )
            }

        layout.addView(adBlock)

        /*
         * COOKIE-BANNERE
         */
        hideCookieBanners =
            CheckBox(this).apply {

                text =
                    "Skjul cookie-bannere"

                isChecked =
                    prefs.getBoolean(
                        "hideCookieBanners_$appWidgetId",
                        prefs.getBoolean(
                            "hideCookieBanners",
                            true
                        )
                    )
            }

        layout.addView(
            hideCookieBanners
        )

        layout.addView(
            TextView(this).apply {

                text =
                    "Forsøger automatisk at fjerne kendte cookie- og samtykke-bannere."

                textSize = 13f

                setPadding(
                    10,
                    0,
                    10,
                    20
                )
            }
        )

        /*
         * OPDATERINGSSTATUS
         */
        showUpdateStatus =
            CheckBox(this).apply {

                text =
                    "Vis opdateringsstatus"

                isChecked =
                    prefs.getBoolean(
                        "showUpdateStatus_$appWidgetId",
                        true
                    )
            }

        layout.addView(
            showUpdateStatus
        )

        layout.addView(
            TextView(this).apply {

                text =
                    "Viser “Opdaterer…” og derefter tidspunktet for den seneste manuelle opdatering."

                textSize = 13f

                setPadding(
                    10,
                    0,
                    10,
                    20
                )
            }
        )

        /*
         * OPDATERINGSINTERVAL
         */
        val refreshValues =
            intArrayOf(
                15,
                30,
                45,
                60,
                120,
                180,
                240,
                300,
                360,
                420,
                480,
                540,
                600,
                660,
                720
            )

        val savedRefreshMinutes =
            prefs.getInt(
                "refreshMinutes_$appWidgetId",
                15
            )

        val refreshLabel =
            TextView(this).apply {
                textSize = 16f
            }

        val refreshSlider =
            SeekBar(this).apply {

                max =
                    refreshValues.size - 1

                progress =
                    refreshValues
                        .indexOf(
                            savedRefreshMinutes
                        )
                        .coerceAtLeast(0)
            }

        fun updateRefreshLabel() {

            val minutes =
                refreshValues[
                    refreshSlider.progress
                ]

            refreshLabel.text =
                if (minutes < 60) {

                    "Opdateringsfrekvens: $minutes minutter"

                } else {

                    val hours =
                        minutes / 60

                    if (hours == 1) {

                        "Opdateringsfrekvens: 1 time"

                    } else {

                        "Opdateringsfrekvens: $hours timer"
                    }
                }
        }

        refreshSlider
            .setOnSeekBarChangeListener(

                object :
                    SeekBar.OnSeekBarChangeListener {

                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {

                        updateRefreshLabel()
                    }

                    override fun onStartTrackingTouch(
                        seekBar: SeekBar?
                    ) {
                    }

                    override fun onStopTrackingTouch(
                        seekBar: SeekBar?
                    ) {
                    }
                }
            )

        updateRefreshLabel()

        layout.addView(refreshLabel)
        layout.addView(refreshSlider)

        /*
         * GEM
         */
        layout.addView(
            Button(this).apply {

                text =
                    "Gem og opdater widget"

                setOnClickListener {

                    val website =
                        url.text
                            .toString()
                            .trim()

                    prefs.edit().apply {

                        if (website.isNotEmpty()) {

                            putString(
                                "url_$appWidgetId",
                                website
                            )

                            remove(
                                "localUri_$appWidgetId"
                            )

                        } else if (
                            localUri != null
                        ) {

                            putString(
                                "localUri_$appWidgetId",
                                localUri.toString()
                            )

                            remove(
                                "url_$appWidgetId"
                            )
                        }

                        /*
                         * Reklameblokering.
                         */
                        putBoolean(
                            "adblock",
                            adBlock.isChecked
                        )

                        /*
                         * Cookie-fjernelse.
                         */
                        putBoolean(
                            "hideCookieBanners_$appWidgetId",
                            hideCookieBanners.isChecked
                        )

                        /*
                         * Beholder global værdi
                         * for kompatibilitet.
                         */
                        putBoolean(
                            "hideCookieBanners",
                            hideCookieBanners.isChecked
                        )

                        /*
                         * Opdateringsstatus
                         * gemmes pr. widget.
                         */
                        putBoolean(
                            "showUpdateStatus_$appWidgetId",
                            showUpdateStatus.isChecked
                        )

                        /*
                         * Opdateringsinterval.
                         */
                        putInt(
                            "refreshMinutes_$appWidgetId",
                            refreshValues[
                                refreshSlider.progress
                            ]
                        )

                        apply()
                    }

                    val refreshMinutes =
                        refreshValues[
                            refreshSlider.progress
                        ]

                    val request =
                        PeriodicWorkRequestBuilder<
                            RefreshWorker
                        >(
                            refreshMinutes.toLong(),
                            TimeUnit.MINUTES
                        )
                            .setInputData(
                                androidx.work.workDataOf(
                                    AppWidgetManager.EXTRA_APPWIDGET_ID to
                                        appWidgetId
                                )
                            )
                            .build()

                    WorkManager
                        .getInstance(
                            this@SetupActivity
                        )
                        .enqueueUniquePeriodicWork(
                            "html-widget-refresh-$appWidgetId",
                            ExistingPeriodicWorkPolicy.UPDATE,
                            request
                        )

                    if (
                        appWidgetId !=
                        AppWidgetManager.INVALID_APPWIDGET_ID
                    ) {

                        HtmlWidgetProvider.refresh(
                            this@SetupActivity,
                            intArrayOf(
                                appWidgetId
                            )
                        )

                        val resultValue =
                            Intent().apply {

                                putExtra(
                                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                                    appWidgetId
                                )
                            }

                        setResult(
                            RESULT_OK,
                            resultValue
                        )

                        Toast.makeText(
                            this@SetupActivity,
                            "Gemt – HTML Widget er opdateret",
                            Toast.LENGTH_LONG
                        ).show()

                        finish()
                    }
                }
            }
        )

        /*
         * SCROLLBAR OPSÆTNINGSSKÆRMEN
         */
        setContentView(

            ScrollView(this).apply {

                addView(layout)
            }
        )
    }

    /*
     * ----------------------------------------
     * HTML-FIL VALGT
     * ----------------------------------------
     */
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

        if (
            requestCode == 42 &&
            resultCode == RESULT_OK
        ) {

            data?.data?.let { uri ->

                contentResolver
                    .takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                localUri =
                    uri

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
