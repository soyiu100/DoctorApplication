package com.doctorapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Patient {

    //todo: add more attr if necessary
    String patientId;
    String firstName;
    String lastName;
}
