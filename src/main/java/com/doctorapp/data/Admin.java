package com.doctorapp.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Admin {
    String userName;
    String emailAddress;
    String firstName;
    String lastName;
}
