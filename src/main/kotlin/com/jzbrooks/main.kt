package com.jzbrooks

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.io.FileTemplateLoader
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.svgsupport.BatikSVGDrawer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.rows
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.jetbrains.kotlinx.dataframe.io.readJson
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

suspend fun main(args: Array<String>) {
    val reader = ArgReader(args.toMutableList())

    if (reader.readFlag("help")) {
        println(USAGE)
        return
    }

    val schedulePath = reader.readOption("schedule")
    if (schedulePath == null) {
        System.err.println("You must specify data with which to populate the template via --schedule")
        return
    }

    val date = reader.readOption("date")
    if (date == null) {
        System.err.println("You must specify a particular the date of the service via --date in MM/DD/YY format")
        return
    }

    val outputPath = (reader.readOption("o|outputPath") ?: DEFAULT_OUTPUT_FILE).toPath()

    val remainingArgs = reader.readArguments()
    if (remainingArgs.size > 1) {
        System.err.println("Warning: expected one argument but got ${remainingArgs.size}")
    }

    val templatePath = remainingArgs.singleOrNull()
    if (templatePath == null) {
        System.err.println("You must specify a template file.")
        return
    }

    val schedule = withContext(Dispatchers.IO) {
        val path = schedulePath.toPath()
        FileSystem.SYSTEM.source(path).buffer().use {
            when (val type = path.toFile().extension) {
                "csv" -> DataFrame.readCSV(it.inputStream())
                "xlsx", "xls" -> DataFrame.readExcel(it.inputStream())
                "json" -> DataFrame.readJson(it.inputStream())
                else -> error("Unexpected schedule file type: $type")
            }
        }
    }

    val week = schedule.rows().firstOrNull { it["Date"] == date }
    if (week == null) {
        System.err.println("Date $date was not found in the schedule.")
        return
    }

    val inputFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    val formattedDate = LocalDate.parse(date, inputFormatter).format(formatter)

    val outputFile = outputPath.toFile()
    withContext(Dispatchers.IO) {
        if (!outputFile.exists()) {
            outputFile.parentFile.mkdirs()
            outputFile.createNewFile()
        }
    }

    val templateFile = templatePath.toPath().toFile().absoluteFile
    val templateFileExtension = templateFile.name.removePrefix(templateFile.nameWithoutExtension)
    val loader = FileTemplateLoader(templateFile.parentFile, templateFileExtension)
    val handlebars = Handlebars(loader)
    val template = handlebars.compile(templateFile.nameWithoutExtension)

    val data = mapOf("DATE" to formattedDate) + csvKeyToTemplateKey.mapNotNull { (csvKey, templateKey) ->
        week.getOrNull(csvKey)?.let { templateKey to it }
    }

    val renderedContent = template.apply(data)

    FileSystem.SYSTEM.sink(outputPath).buffer().use {
        PdfRendererBuilder()
            .withHtmlContent(renderedContent, null)
            .useSVGDrawer(BatikSVGDrawer())
            .toStream(it.outputStream())
            .run()
    }
}

private const val DEFAULT_OUTPUT_FILE = "output/out.pdf"

val csvKeyToTemplateKey = mapOf(
    "Hymn 1" to "HYMN_1",
    "Hymn 2" to "HYMN_2",
    "Hymn 3" to "HYMN_3",
    "Hymn 4" to "HYMN_4",
    "Hymn 5" to "HYMN_5",
    "Hymn 6" to "HYMN_6",
    "Hymn 7" to "HYMN_7",
    "Scripture" to "SCRIPTURE",
    "Prayer Verse" to "PRAYER_VERSE",
    "Assurance Verse" to "ASSURANCE_VERSE",
    "Catechism Question" to "CATECHISM_QUESTION",
    "Catechism Answer" to "CATECHISM_ANSWER",
    "Catechism Scripture References" to "CATECHISM_SCRIPTURE",
    "Benediction" to "BENEDICTION",
    "Benediction Scripture" to "BENEDICTION_SCRIPTURE",
    "OT Reading" to "OT_READING",
    "NT Reading" to "NT_READING",
    "Baptisms" to "BAPTISMS",
)

private val USAGE = """
    liturgist <options> template.html 
    
    A pdf generator for liturgical events.
    
    --date:     A date on the schedule to select data for the template replacement

    --help: Print usage

    -o --output (optional): A path to the output pdf (default path is $DEFAULT_OUTPUT_FILE)
    
    --template: A path to a moustache template
                Template names: ${csvKeyToTemplateKey.values}

    --schedule: A path to a schedule - csv, json, and xlsx are supported
                Column names: ${csvKeyToTemplateKey.keys}
""".trimIndent()
