/**
 * Copyright (c) 2023 Natalia Molinero Mingorance
 * All rights reserved.
 */

package harmony.valley.wamboocam

import android.content.ContentResolver
import android.content.Context
import android.content.DialogInterface.OnClickListener
import android.graphics.DashPathEffect							  
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import harmony.valley.wamboocam.models.FinalChartData
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.log10
import kotlin.math.pow

// This function returns the file size in human readable format . Like it will take in size and return the size in kb or mb which can be
// displayed to the user .
fun fileSize(size2: Long): String {
    if (size2 <= 0) return "0"
    val units = arrayOf("B", "kB", "MB", "GB", "TB")
    val digitGroups = (log10(size2.toDouble()) / log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(
        size2 / 1024.0.pow(digitGroups.toDouble())
    ) + " " + units[digitGroups]
}

/* function that returns the length of a file specified by a URI.
The function first tries to get the length of the file by opening an asset file descriptor and calling the length method.
If the length can't be obtained this way, the function checks if the scheme of the URI is "content://",
and if so, it tries to get the length by querying the content resolver table.
If either of these methods fails, the function returns -1.  */
fun Uri.length(contentResolver: ContentResolver): Long {

    val assetFileDescriptor = try {
        contentResolver.openAssetFileDescriptor(this, "r")
    } catch (e: Exception) {
        null
    }
    // uses ParcelFileDescriptor#getStatSize underneath if failed
    val length = assetFileDescriptor?.use { it.length } ?: -1L
    if (length != -1L) {
        return length
    }

    // if "content://" uri scheme, try contentResolver table
    if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
        return contentResolver.query(this, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                // maybe shouldn't trust ContentResolver for size: https://stackoverflow.com/questions/48302972/content-resolver-returns-wrong-size
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex == -1) {
                    return@use -1L
                }
                cursor.moveToFirst()
                return try {
                    cursor.getLong(sizeIndex)
                } catch (_: Throwable) {
                    -1L
                }
            } ?: -1L
    } else {
        return -1L
    }
}
fun getCalenderObject(
    year: Int,
    month: Int,
    Day: Int,
    start: Boolean = true
): Calendar {
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, Day)
        if (start) {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        } else {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }
    }
}

fun showMessage(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
}

fun showAlertDialog(context: Context, listener: OnClickListener) {
    AlertDialog.Builder(context).setTitle(context.resources.getString(R.string.delete_data)+"!").setMessage(context.resources.getString(R.string.delete_message))
        .setPositiveButton(
            context.resources.getString(R.string.yes), listener
        ).setNegativeButton(context.resources.getString(R.string.no)) { p0, _ ->
            p0?.dismiss()
        }.setCancelable(false).create().show()
}



fun setUpLineChart(
    mChart: LineChart,
    finalChartData: List<FinalChartData>,
    isFileChart: Boolean = false
) {

    val min = finalChartData.minOfOrNull { it.value }
    val max = finalChartData.maxOfOrNull { it.value }

    val valuesList = ArrayList(finalChartData.mapIndexed { index, item ->
        Entry(index.toFloat(), item.value.toFloat())
    })


    with(mChart) {
        refreshDrawableState()
        setTouchEnabled(false)
        setPinchZoom(false)
        extraRightOffset = 20f
        extraLeftOffset = 5f
        setScaleEnabled(false)
        legend.isEnabled = false


        val xAxis: XAxis = mChart.xAxis
        xAxis.granularity = 1f
        if (valuesList.size < 5) {
            xAxis.setLabelCount(valuesList.size, true)
        } else {
            xAxis.setLabelCount(5, true)
        }

        xAxis.setCenterAxisLabels(valuesList.size == 1)

        xAxis.spaceMax = 0.1f
        xAxis.spaceMin = 0.1f
        xAxis.textSize = 9f
        xAxis.isEnabled = true
        xAxis.setDrawGridLines(false)
        xAxis.position = XAxis.XAxisPosition.BOTTOM

        val yAxis: YAxis = mChart.axisLeft
        yAxis.setCenterAxisLabels(true)
        yAxis.axisMinimum = if (valuesList.size == 1) 0f else min?.toFloat() ?: 0f
        yAxis.axisMaximum = max?.toFloat() ?: 0f

        if (valuesList.size < 5) {
            yAxis.setLabelCount(valuesList.size, true)
        } else {
            yAxis.setLabelCount(5, true)
        }

        if (isFileChart) {
            yAxis.valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    return String.format("%.2f", value)
                }
            }
        }

        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                return try {
                    finalChartData[value.toInt()].date
                } catch (e: Exception) {
                    ""
                }
            }
        }
    }


    val set1 = LineDataSet(valuesList, "")
    set1.setDrawIcons(false)
    set1.enableDashedLine(10f, 5f, 0f)
    set1.enableDashedHighlightLine(10f, 5f, 0f)
    /*set1.color = Color.DKGRAY
    set1.setCircleColor(Color.DKGRAY)*/
    set1.lineWidth = 1f
    set1.circleRadius = 3f
    set1.setDrawCircleHole(false)
    set1.valueTextSize = 9f
    set1.setDrawFilled(false)
    set1.formLineWidth = 1f
    set1.formLineDashEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
    set1.formSize = 15f

    val dataSets: ArrayList<ILineDataSet> = ArrayList()
    dataSets.add(set1)
    val data = LineData(dataSets)

    mChart.axisRight.isEnabled = false
    mChart.description.isEnabled = false

    mChart.data = data
    mChart.notifyDataSetChanged()
    mChart.invalidate()
}