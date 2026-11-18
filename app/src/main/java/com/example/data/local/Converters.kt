package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.OperationType
import com.example.data.model.PartnerType
import com.example.data.model.StockMovementType

class Converters {
    @TypeConverter
    fun fromOperationType(value: OperationType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toOperationType(value: String?): OperationType? {
        return value?.let {
            try {
                OperationType.valueOf(it)
            } catch (e: Exception) {
                OperationType.BL_CLIENT
            }
        }
    }

    @TypeConverter
    fun fromPartnerType(value: PartnerType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toPartnerType(value: String?): PartnerType? {
        return value?.let {
            try {
                PartnerType.valueOf(it)
            } catch (e: Exception) {
                PartnerType.CLIENT
            }
        }
    }

    @TypeConverter
    fun fromStockMovementType(value: StockMovementType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toStockMovementType(value: String?): StockMovementType? {
        return value?.let {
            try {
                StockMovementType.valueOf(it)
            } catch (e: Exception) {
                StockMovementType.ENTREE
            }
        }
    }
}
