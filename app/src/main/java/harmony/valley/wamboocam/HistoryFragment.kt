/**
 * Copyright (c) 2023 Natalia Molinero Mingorance
 * All rights reserved.
 */

package harmony.valley.wamboocam

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.AndroidEntryPoint
import harmony.valley.wamboocam.databinding.FragmentHistoryBinding
import harmony.valley.wamboocam.databinding.RowChartBinding
import harmony.valley.wamboocam.models.*
import harmony.valley.wamboocam.vm.CompressViewModel
import java.math.RoundingMode
import java.util.*
//import android.R


@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private lateinit var binding: FragmentHistoryBinding
    lateinit var viewModel: CompressViewModel
    var pollutionAverage = 0.0
    lateinit var chartAdapter: ChartAdapter
    private lateinit var mAdView3: AdView
    companion object {
        fun newInstance() = HistoryFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentHistoryBinding.inflate(inflater, container, false).run {
        binding = this

        MobileAds.initialize(requireActivity()) {}
        val adRequest = AdRequest.Builder().build()
        mAdView3 = binding.root.findViewById(R.id.adView3)
        //mAdView3.setAdSize(AdSize.BANNER)
        //mAdView3.setAdUnitId("ca-app-pub-6983852485981992/8859021238")
        mAdView3.loadAd(adRequest)
        return@run this.root
    }

    private fun showDatePicker(text: String, onDateSetListener: OnDateSetListener) {
        val calender = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            requireContext(),
            onDateSetListener,
            calender.get(Calendar.YEAR),
            calender.get(Calendar.MONTH),
            calender.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.setOnShowListener {
            datePicker.getButton(DatePickerDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.teal_700))
            datePicker.getButton(DatePickerDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
        }

        datePicker.setTitle(text)
        datePicker.show()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chartAdapter = ChartAdapter(
            onNext = {
                binding.rvItems.smoothScrollToPosition(it + 1)
            }, onPrev = {
                binding.rvItems.smoothScrollToPosition(it - 1)
            }, requireContext(), arrayListOf()
        )
        binding.rvItems.adapter = chartAdapter
        binding.rvItems.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        viewModel = ViewModelProvider(this)[CompressViewModel::class.java]

        viewModel.getCount().observe(viewLifecycleOwner) {
            binding.tvNoData.isVisible = it.isEmpty()
            binding.rootLayout.isVisible = it.isEmpty().not()
        }

        binding.delete.setOnClickListener {

            showAlertDialog(requireContext()) { p0, p1 ->
                p0?.dismiss()
                viewModel.delete()
                showMessage(requireContext(), getString(R.string.delete_sucessful))
            }

        }

        binding.rgTypes.setOnCheckedChangeListener { radioGroup, i ->
            /*val km = (pollutionAverage/0.140).toBigDecimal().setScale(2,
                RoundingMode.UP).toDouble()
            binding.equivalence.text =getString(R.string.equivalences)+"$km"+"km"*/
            val isDailySelected = radioGroup.checkedRadioButtonId == R.id.rbDaily
            binding.rvItems.isVisible = isDailySelected
            binding.finalLayout.isVisible = isDailySelected.not()


        }

        binding.deleteSpec.setOnClickListener {

            startFilter {
                showAlertDialog(requireContext()) { p0, p1 ->
                    viewModel.deleteSpecific(it.first, it.second)
                    showMessage(requireContext(), getString(R.string.delete_sucessful))
                }
            }
        }



        viewModel.userLiveDataByDate.observe(viewLifecycleOwner) { compressList ->
            val data = compressList.sortedBy { it.milliSeconds }


            binding.chartLayout.isVisible = compressList.isNotEmpty()

            val compressChartViewList = kotlin.collections.ArrayList<CompressChartView>()

            val finalChartListPollution = kotlin.collections.ArrayList<FinalChartData>()
            val finalChartListFile = kotlin.collections.ArrayList<FinalChartData>()


            data.groupBy { it.date }.forEach { (date, compressData) ->
                compressChartViewList.add(CompressChartView(date, compressData))

                pollutionAverage = compressData.map { it.co2 }.average()
                val mbAverage =
                    compressData.map { (it.sizeReduction).toFloat()}.average()

                finalChartListPollution.add(FinalChartData(pollutionAverage, date))
                finalChartListFile.add(FinalChartData(mbAverage, date))
            }


            val rowBinding = RowChartBinding.bind(binding.finalLayout)
            rowBinding.date.isVisible = false
            setUpLineChart(rowBinding.lineChart, finalChartListPollution)
            setUpLineChart(rowBinding.chartFile, finalChartListFile, true)
            val km = (pollutionAverage/0.140).toBigDecimal().setScale(2,
                RoundingMode.UP).toDouble()
            binding.equivalence.text = buildString {
        append(getString(R.string.equivalences))
        append(km)
        append("km")
    }
            chartAdapter.updateData(compressChartViewList)
        }

        binding.fetch.setOnClickListener {
            startFilter {
                viewModel.setMap(it.first, it.second)
            }
        }

    }


    private fun startFilter(filter: (Pair<Long, Long>) -> Unit) {
        showDatePicker(getString(R.string.select_init_date)) { p0, year, month, day ->
            val startCalendar = getCalenderObject(year, month, day)
            showDatePicker(getString(R.string.select_final_date)) { p0, endYear, endMonth, endDay ->
                val endCalendar = getCalenderObject(endYear, endMonth, endDay, false)

                if (startCalendar.after(endCalendar)) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.date_messages),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    filter(Pair(startCalendar.time.time, endCalendar.time.time))
                }
            }
        }
    }

}