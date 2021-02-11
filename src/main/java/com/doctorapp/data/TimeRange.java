package com.doctorapp.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimeRange {
    String startTime;
    String endTime;
}
