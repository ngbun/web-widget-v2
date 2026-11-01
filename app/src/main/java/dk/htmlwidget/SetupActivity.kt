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
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SetupActivity : Activity() {

    private val prefs by lazy {
        getSharedPreferences(
            "settings",
            MODE_PRIVATE
        )
    }

    private var localUri: Uri? = null

    private lateinit var widgetName: EditText
    private lateinit var url: EditText
    private lateinit var adBlock: CheckBox
    private lateinit var hideCookieBanners: CheckBox
    private lateinit var showUpdateStatus: CheckBox

    private lateinit var refreshSlider: SeekBar
    private lateinit var refreshLabel: TextView

    private val refreshValues =
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

    private var appWidgetId =
        AppWidgetManager.INVALID_APPWIDGET_ID

    private val chooseHtmlRequest = 42
    private val exportBackupRequest = 43
    private val importBackupRequest = 44

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
         * WIDGET-NAVN
         */
        widgetName =
            EditText(this).apply {

                hint = "Navn på widget, fx Elpris"

                setText(
                    prefs.getString(
                        "widgetName_$appWidgetId",
                        ""
                    )
                )

                setSingleLine(true)
            }

        layout.addView(
            widgetName
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

        layout.addView(
            url
        )

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

                    if (
                        website.isEmpty()
                    ) {

                        Toast.makeText(
                            this@SetupActivity,
                            "Indtast først en hjemmesideadresse",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@setOnClickListener
                    }

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

                    startActivity(
                        cropIntent
                    )
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
                        chooseHtmlRequest
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

        layout.addView(
            adBlock
        )

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
        val savedRefreshMinutes =
            prefs.getInt(
                "refreshMinutes_$appWidgetId",
                15
            )

        refreshLabel =
            TextView(this).apply {

                textSize = 16f
            }

        refreshSlider =
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

        layout.addView(
            refreshLabel
        )

                layout.addView(
            refreshSlider
        )

        /*
         * PREVIEW
         */
        layout.addView(
            Button(this).apply {

                text =
                    "Vis preview"

                setOnClickListener {

                    val previewIntent =
                        Intent(
                            this@SetupActivity,
                            WidgetPreviewActivity::class.java
                        ).apply {

                            putExtra(
                                AppWidgetManager.EXTRA_APPWIDGET_ID,
                                appWidgetId
                            )
                        }

                    startActivity(
                        previewIntent
                    )
                }
            }
        )

        /*
         * ----------------------------------------
         * BACKUP
         * ----------------------------------------
         */
        layout.addView(
            TextView(this).apply {

                text = "Backup"
                textSize = 20f

                setPadding(
                    0,
                    30,
                    0,
                    5
                )
            }
        )

        layout.addView(
            TextView(this).apply {

                text =
                    "Gem eller gendan indstillingerne for denne widget."

                textSize = 13f

                setPadding(
                    0,
                    0,
                    0,
                    10
                )
            }
        )

        /*
         * EKSPORT
         */
        layout.addView(
            Button(this).apply {

                text =
                    "Eksporter backup"

                setOnClickListener {

                    exportBackup()
                }
            }
        )

        /*
         * IMPORT
         */
        layout.addView(
            Button(this).apply {

                text =
                    "Importer backup"

                setOnClickListener {

                    importBackup()
                }
            }
        )

        /*
         * GEM
         */
        layout.addView(
            Button(this).apply {

                text =
                    "Gem og opdater widget"

                setOnClickListener {

                    saveWidgetSettings()
                }
            }
        )

        /*
         * SCROLLBAR
         */
        setContentView(

            ScrollView(this).apply {

                addView(
                    layout
                )
            }
        )
    }

    /*
     * ----------------------------------------
     * OPDATER REFRESH-TEKST
     * ----------------------------------------
     */
    private fun updateRefreshLabel() {

        val minutes =
            refreshValues[
                refreshSlider.progress
            ]

        refreshLabel.text =
            if (
                minutes < 60
            ) {

                "Opdateringsfrekvens: $minutes minutter"

            } else {

                val hours =
                    minutes / 60

                if (
                    hours == 1
                ) {

                    "Opdateringsfrekvens: 1 time"

                } else {

                    "Opdateringsfrekvens: $hours timer"
                }
            }
    }

    /*
     * ----------------------------------------
     * GEM WIDGET
     * ----------------------------------------
     */
    private fun saveWidgetSettings() {

        val widgetNameValue =
            widgetName.text
                .toString()
                .trim()

        val website =
            url.text
                .toString()
                .trim()

        prefs.edit().apply {

            putString(
                "widgetName_$appWidgetId",
                widgetNameValue
            )

            if (
                website.isNotEmpty()
            ) {

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

            putBoolean(
                "adblock",
                adBlock.isChecked
            )

            putBoolean(
                "hideCookieBanners_$appWidgetId",
                hideCookieBanners.isChecked
            )

            putBoolean(
                "hideCookieBanners",
                hideCookieBanners.isChecked
            )

            putBoolean(
                "showUpdateStatus_$appWidgetId",
                showUpdateStatus.isChecked
            )

            putInt(
                "refreshMinutes_$appWidgetId",
                refreshValues[
                    refreshSlider.progress
                ]
            )

            apply()
        }

        scheduleRefresh()

        if (
            appWidgetId !=
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) {

            HtmlWidgetProvider.refresh(
                this,
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
                this,
                "Gemt – HTML Widget er opdateret",
                Toast.LENGTH_LONG
            ).show()

            finish()
        }
    }

    /*
     * ----------------------------------------
     * PLANLÆG AUTOMATISK REFRESH
     * ----------------------------------------
     */
    private fun scheduleRefresh() {

        if (
            appWidgetId ==
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) {
            return
        }

        val refreshMinutes =
            prefs.getInt(
                "refreshMinutes_$appWidgetId",
                15
            )

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
                this
            )
            .enqueueUniquePeriodicWork(
                "html-widget-refresh-$appWidgetId",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
    }

    /*
     * ----------------------------------------
     * EKSPORTER BACKUP
     * ----------------------------------------
     */
    private fun exportBackup() {

        if (
            appWidgetId ==
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) {

            Toast.makeText(
                this,
                "Kunne ikke finde widgetten",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val intent =
            Intent(
                Intent.ACTION_CREATE_DOCUMENT
            ).apply {

                addCategory(
                    Intent.CATEGORY_OPENABLE
                )

                type =
                    "application/json"

                putExtra(
                    Intent.EXTRA_TITLE,
                    "web-widget-backup-$appWidgetId.json"
                )
            }

        startActivityForResult(
            intent,
            exportBackupRequest
        )
    }

    /*
     * ----------------------------------------
     * IMPORTER BACKUP
     * ----------------------------------------
     */
    private fun importBackup() {

        if (
            appWidgetId ==
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) {

            Toast.makeText(
                this,
                "Kunne ikke finde widgetten",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val intent =
            Intent(
                Intent.ACTION_OPEN_DOCUMENT
            ).apply {

                addCategory(
                    Intent.CATEGORY_OPENABLE
                )

                type =
                    "application/json"

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        startActivityForResult(
            intent,
            importBackupRequest
        )
    }

    /*
     * ----------------------------------------
     * LAV BACKUP-JSON
     * ----------------------------------------
     */
    private fun createBackupJson(): JSONObject {

        val backup =
            JSONObject()

        backup.put(
            "backupVersion",
            1
        )

        backup.put(
            "app",
            "Web Widget"
        )

        backup.put(
            "adblock",
            prefs.getBoolean(
                "adblock",
                true
            )
        )

        val widget =
            JSONObject()

        fun putString(
            name: String
        ) {

            val key =
                "${name}_$appWidgetId"

            if (
                prefs.contains(
                    key
                )
            ) {

                widget.put(
                    name,
                    prefs.getString(
                        key,
                        ""
                    )
                )
            }
        }

        fun putBoolean(
            name: String
        ) {

            val key =
                "${name}_$appWidgetId"

            if (
                prefs.contains(
                    key
                )
            ) {

                widget.put(
                    name,
                    prefs.getBoolean(
                        key,
                        false
                    )
                )
            }
        }

        fun putInt(
            name: String
        ) {

            val key =
                "${name}_$appWidgetId"

            if (
                prefs.contains(
                    key
                )
            ) {

                widget.put(
                    name,
                    prefs.getInt(
                        key,
                        0
                    )
                )
            }
        }

        fun putFloat(
            name: String
        ) {

            val key =
                "${name}_$appWidgetId"

            if (
                prefs.contains(
                    key
                )
            ) {

                widget.put(
                    name,
                    prefs.getFloat(
                        key,
                        0f
                    ).toDouble()
                )
            }
        }

        putString("widgetName")
        putString("url")
        putString("localUri")

        putBoolean("hideCookieBanners")
        putBoolean("showUpdateStatus")

        putInt("refreshMinutes")

        putBoolean("cropEnabled")

        putFloat("cropCssX")
        putFloat("cropCssY")
        putFloat("cropCssWidth")
        putFloat("cropCssHeight")

        putFloat("cropCssViewportWidth")
        putFloat("cropCssViewportHeight")

        putFloat("cropCssDocumentWidth")
        putFloat("cropCssDocumentHeight")

        putInt("cropViewportWidth")
        putInt("cropViewportHeight")

        putFloat("cropWebScale")

        putInt("cropSelectionWidth")
        putInt("cropSelectionHeight")

        putInt("websiteScrollX")
        putInt("websiteScrollY")

        putFloat("cropFrameX")
        putFloat("cropFrameY")

        putInt("cropFrameWidth")
        putInt("cropFrameHeight")

        backup.put(
            "widget",
            widget
        )
    

        return backup
    }
        /*
     * ----------------------------------------
     * SKRIV BACKUP
     * ----------------------------------------
     */
    private fun writeBackup(
        uri: Uri
    ) {

        try {

            val json =
                createBackupJson()
                    .toString(2)

            contentResolver
                .openOutputStream(
                    uri
                )
                ?.bufferedWriter()
                ?.use {

                    it.write(
                        json
                    )
                }
                ?: throw Exception(
                    "Kunne ikke åbne filen"
                )

            Toast.makeText(
                this,
                "Backup gemt",
                Toast.LENGTH_LONG
            ).show()

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Kunne ikke gemme backup",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /*
     * ----------------------------------------
     * LÆS OG IMPORTER BACKUP
     * ----------------------------------------
     */
    private fun readBackup(
        uri: Uri
    ) {

        try {

            val jsonText =
                contentResolver
                    .openInputStream(
                        uri
                    )
                    ?.bufferedReader()
                    ?.use {

                        it.readText()
                    }
                    ?: throw Exception(
                        "Kunne ikke læse filen"
                    )

            val backup =
                JSONObject(
                    jsonText
                )

            val backupVersion =
                backup.optInt(
                    "backupVersion",
                    -1
                )

            if (
                backupVersion != 1
            ) {

                Toast.makeText(
                    this,
                    "Backup-versionen understøttes ikke",
                    Toast.LENGTH_LONG
                ).show()

                return
            }

            val widget =
                backup.optJSONObject(
                    "widget"
                )
                    ?: throw Exception(
                        "Widget-data mangler"
                    )

            val editor =
                prefs.edit()

            /*
             * Global reklameblokering.
             */
            if (
                backup.has(
                    "adblock"
                )
            ) {

                editor.putBoolean(
                    "adblock",
                    backup.optBoolean(
                        "adblock",
                        true
                    )
                )
            }

            /*
             * Hjælpefunktioner.
             */
            fun importString(
                name: String
            ) {

                if (
                    widget.has(
                        name
                    ) &&
                    !widget.isNull(
                        name
                    )
                ) {

                    editor.putString(
                        "${name}_$appWidgetId",
                        widget.optString(
                            name,
                            ""
                        )
                    )
                }
            }

            fun importBoolean(
                name: String
            ) {

                if (
                    widget.has(
                        name
                    )
                ) {

                    editor.putBoolean(
                        "${name}_$appWidgetId",
                        widget.optBoolean(
                            name,
                            false
                        )
                    )
                }
            }

            fun importInt(
                name: String
            ) {

                if (
                    widget.has(
                        name
                    )
                ) {

                    editor.putInt(
                        "${name}_$appWidgetId",
                        widget.optInt(
                            name,
                            0
                        )
                    )
                }
            }

            fun importFloat(
                name: String
            ) {

                if (
                    widget.has(
                        name
                    )
                ) {

                    editor.putFloat(
                        "${name}_$appWidgetId",
                        widget.optDouble(
                            name,
                            0.0
                        ).toFloat()
                    )
                }
            }

            /*
             * Fjern først gammel URL / lokal fil,
             * så vi ikke ender med begge dele.
             */
            editor.remove(
                "url_$appWidgetId"
            )

            editor.remove(
                "localUri_$appWidgetId"
            )

            /*
             * Navn.
             */
            importString(
                "widgetName"
            )

            /*
             * Adresse / lokal HTML.
             */
            importString(
                "url"
            )

            importString(
                "localUri"
            )

            /*
             * Almindelige indstillinger.
             */
            importBoolean(
                "hideCookieBanners"
            )

            importBoolean(
                "showUpdateStatus"
            )

            importInt(
                "refreshMinutes"
            )

            /*
             * Crop.
             */
            importBoolean(
                "cropEnabled"
            )

            importFloat(
                "cropCssX"
            )

            importFloat(
                "cropCssY"
            )

            importFloat(
                "cropCssWidth"
            )

            importFloat(
                "cropCssHeight"
            )

            importFloat(
                "cropCssViewportWidth"
            )

            importFloat(
                "cropCssViewportHeight"
            )

            importFloat(
                "cropCssDocumentWidth"
            )

            importFloat(
                "cropCssDocumentHeight"
            )

            importInt(
                "cropViewportWidth"
            )

            importInt(
                "cropViewportHeight"
            )

            importFloat(
                "cropWebScale"
            )

            importInt(
                "cropSelectionWidth"
            )

            importInt(
                "cropSelectionHeight"
            )

            importInt(
                "websiteScrollX"
            )

            importInt(
                "websiteScrollY"
            )

            importFloat(
                "cropFrameX"
            )

            importFloat(
                "cropFrameY"
            )

            importInt(
                "cropFrameWidth"
            )

            importInt(
                "cropFrameHeight"
            )

            /*
             * En gammel checksum skal IKKE
             * genbruges efter import.
             *
             * Vi vil have første rendering til
             * at sende et nyt billede.
             */
            editor.remove(
                "lastBitmapChecksum_$appWidgetId"
            )

            editor.remove(
                "lastSuccessfulUpdate_$appWidgetId"
            )

            editor.apply()

            /*
             * Global kompatibilitetsværdi.
             */
            val importedCookieSetting =
                prefs.getBoolean(
                    "hideCookieBanners_$appWidgetId",
                    true
                )

            prefs.edit()
                .putBoolean(
                    "hideCookieBanners",
                    importedCookieSetting
                )
                .apply()

            /*
             * Opdater skærmens felter med
             * de importerede værdier.
             */
            updateUiAfterImport()

            /*
             * Start det importerede interval.
             */
            scheduleRefresh()

            /*
             * Render widgetten med de
             * importerede indstillinger.
             */
            HtmlWidgetProvider.refresh(
                this,
                intArrayOf(
                    appWidgetId
                )
            )

            Toast.makeText(
                this,
                "Backup importeret",
                Toast.LENGTH_LONG
            ).show()

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Kunne ikke importere backup",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /*
     * ----------------------------------------
     * OPDATER SKÆRM EFTER IMPORT
     * ----------------------------------------
     */
    private fun updateUiAfterImport() {

        val importedWidgetName =
            prefs.getString(
                "widgetName_$appWidgetId",
                ""
            ) ?: ""

        widgetName.setText(
            importedWidgetName
        )

        val importedUrl =
            prefs.getString(
                "url_$appWidgetId",
                ""
            ) ?: ""

        url.setText(
            importedUrl
        )

        val importedLocalUri =
            prefs.getString(
                "localUri_$appWidgetId",
                null
            )

        localUri =
            if (
                importedLocalUri.isNullOrBlank()
            ) {

                null

            } else {

                try {

                    Uri.parse(
                        importedLocalUri
                    )

                } catch (_: Exception) {

                    null
                }
            }

        adBlock.isChecked =
            prefs.getBoolean(
                "adblock",
                true
            )

        hideCookieBanners.isChecked =
            prefs.getBoolean(
                "hideCookieBanners_$appWidgetId",
                true
            )

        showUpdateStatus.isChecked =
            prefs.getBoolean(
                "showUpdateStatus_$appWidgetId",
                true
            )

        val importedRefresh =
            prefs.getInt(
                "refreshMinutes_$appWidgetId",
                15
            )

        val importedIndex =
            refreshValues
                .indexOf(
                    importedRefresh
                )

        refreshSlider.progress =
            if (
                importedIndex >= 0
            ) {

                importedIndex

            } else {

                0
            }

        updateRefreshLabel()
    }

    /*
     * ----------------------------------------
     * RESULTATER
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

        /*
         * HTML-FIL VALGT
         */
        if (
            requestCode ==
            chooseHtmlRequest &&
            resultCode ==
            RESULT_OK
        ) {

            data?.data?.let { uri ->

                try {

                    contentResolver
                        .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                } catch (_: Exception) {
                }

                localUri =
                    uri

                url.setText("")

                Toast.makeText(
                    this,
                    "HTML-fil valgt",
                    Toast.LENGTH_SHORT
                ).show()
            }

            return
        }

        /*
         * EKSPORT
         */
        if (
            requestCode ==
            exportBackupRequest &&
            resultCode ==
            RESULT_OK
        ) {

            data?.data?.let { uri ->

                writeBackup(
                    uri
                )
            }

            return
        }

        /*
         * IMPORT
         */
        if (
            requestCode ==
            importBackupRequest &&
            resultCode ==
            RESULT_OK
        ) {

            data?.data?.let { uri ->

                readBackup(
                    uri
                )
            }
        }
    }
}
