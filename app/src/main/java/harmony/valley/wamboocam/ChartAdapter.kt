/**
 * Copyright (c) 2023 Natalia Molinero Mingorance
 * All rights reserved.
 */

package harmony.valley.wamboocam

import android.content.Context
import android.graphics.DashPathEffect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import harmony.valley.wamboocam.models.CompressChartView
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class ChartAdapter(
    var onNext: (Int) -> Unit,
    var onPrev: (Int) -> Unit,
    val context: Context,
    private var items: ArrayList<CompressChartView>
) :
    RecyclerView.Adapter<ChartAdapter.ViewHolder>() {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        setUpLineChart(holder.chart, items[position])
        setUpLineChart(holder.chartFile, items[position], true)
        holder.date.text = items[position].date

        if (items.size < 2) {
            holder.ivNext.visibility = View.GONE
            holder.ivPrevious.visibility = View.GONE
        } else {
            if (items.size > position + 1) {
                holder.ivNext.visibility = View.VISIBLE
            } else {
                holder.ivNext.visibility = View.GONE
            }

            if (position > 0) {
                holder.ivPrevious.visibility = View.VISIBLE
            } else {
                holder.ivPrevious.visibility = View.GONE
            }
        }

        holder.ivNext.setOnClickListener {
            onNext(position)
        }

        holder.ivPrevious.setOnClickListener {
            onPrev(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater: LayoutInflater = LayoutInflater.from(parent.context)
        val listItem: View = layoutInflater.inflate(R.layout.row_chart, parent, false)
        return ViewHolder(listItem)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var chart: LineChart = itemView.findViewById(R.id.lineChart) as LineChart
        var chartFile: LineChart = itemView.findViewById(R.id.chartFile) as LineChart
        var date: TextView = itemView.findViewById(R.id.date) as TextView
        var ivNext: AppCompatImageView =
            itemView.findViewById(R.id.ivNextIcon) as AppCompatImageView
        var ivPrevious: AppCompatImageView =
            itemView.findViewById(R.id.ivPrevIcon) as AppCompatImageView
    }

    private fun setUpLineChart(
        mChart: LineChart,
        compressChartView: CompressChartView,
        isFileChart: Boolean = false
    ) {

        val compressData = compressChartView.compressDataList

        with(mChart) {
            refreshDrawableState()
            setTouchEnabled(false)
            setPinchZoom(false)
            extraRightOffset = 20f
            setScaleEnabled(false)
            legend.isEnabled = false

            val xAxis: XAxis = mChart.xAxis
            if (compressData.size < 5) {
                xAxis.setLabelCount(compressData.size, true)
            } else {
                xAxis.setLabelCount(5, true)
            }

            xAxis.spaceMax = 0.1f
            xAxis.spaceMin = 0.1f
            xAxis.textSize = 9f
            xAxis.isEnabled = true
            xAxis.setDrawGridLines(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM


                val leftAxis = mChart.axisLeft
                leftAxis.valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                        return "${
                            value.toBigDecimal().setScale(2,
                                RoundingMode.UP)
                        }"
                    }
                }

            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    return try {
                        SimpleDateFormat(
                            "hh:mm a",
                            Locale.ENGLISH
                        ).format(Date(compressData[value.toInt()].milliSeconds))
                    } catch (e: Exception) {
                        ""
                    }
                }
            }
        }

        val date = compressChartView.date
        val values = ArrayList(compressData.mapIndexed { index, item ->
            Entry(
                index.toFloat(),
                if (isFileChart) ((item.sizeReduction.toFloat())) else item.co2.toFloat()
            )
        })


        val set1 = LineDataSet(values, date)
        set1.setDrawIcons(false)
        set1.enableDashedLine(10f, 5f, 0f)
        set1.enableDashedHighlightLine(10f, 5f, 0f)
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
        mChart.axisLeft.axisMinimum = "0".toFloat()
        mChart.data = data
    }

    fun updateData(data: ArrayList<CompressChartView>) {
        for (item in items) {
            repeat(item.compressDataList.size) {
            }
        }
        items.apply {
            clear()
            addAll(data)
        }
        notifyDataSetChanged()
    }

}

