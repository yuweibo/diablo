package com.ishansong.diablo.admin.query;

import com.ishansong.diablo.admin.page.PageParameter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardUserQuery implements Serializable {

    private String userName;

    private PageParameter pageParameter;
}
