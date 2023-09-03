/**
 * Copyright (c) 2023 Natalia Molinero Mingorance
 * All rights reserved.
 */

package harmony.valley.wamboocam.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "compression")
class CompressData(
    val sizeReduction: Long,
    val co2: Long,
    val milliSeconds: Long,
    val date: String,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

data class CompressChartView(val date: String, val compressDataList: List<CompressData>)

data class FinalChartData(val value: Double, var date: String)