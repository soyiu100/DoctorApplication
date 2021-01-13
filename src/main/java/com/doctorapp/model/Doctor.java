package com.doctorapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Doctor {
    String userName;
    String emailAddress;
    String firstName;
    String lastName;
    String title;
    String partnerName;
}