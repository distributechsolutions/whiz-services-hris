package io.distributechsolutions.hris.views.profile;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.*;

import io.distributechsolutions.hris.dtos.reference.PositionDTO;
import io.distributechsolutions.hris.dtos.profile.EmployeeDTO;
import io.distributechsolutions.hris.dtos.profile.EmployeePositionDTO;
import io.distributechsolutions.hris.services.reference.PositionService;
import io.distributechsolutions.hris.services.profile.EmployeePositionService;
import io.distributechsolutions.hris.services.profile.EmployeeService;
import io.distributechsolutions.hris.utils.SecurityUtil;
import io.distributechsolutions.hris.views.MainLayout;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;

import java.util.Objects;
import java.util.UUID;

@RolesAllowed({"ROLE_ADMIN",
        "ROLE_HR_MANAGER",
        "ROLE_HR_SUPERVISOR",
        "ROLE_HR_EMPLOYEE"})
@PageTitle("Employee Position Form")
@Route(value = "employee-position-form", layout = MainLayout.class)
public class EmployeePositionFormView extends VerticalLayout implements HasUrlParameter<String> {
    @Resource
    private final EmployeePositionService employeePositionService;
    @Resource
    private final EmployeeService employeeService;
    @Resource
    private final PositionService positionService;

    private EmployeePositionDTO employeePositionDTO;
    private UUID parameterId;

    private final FormLayout employeePositionDTOFormLayout = new FormLayout();
    private ComboBox<EmployeeDTO> employeeDTOComboBox;
    private ComboBox<PositionDTO> positionDTOComboBox;
    private Checkbox currentPositionCheckbox;

    public EmployeePositionFormView(EmployeePositionService employeePositionService,
                                    EmployeeService employeeService,
                                    PositionService positionService) {
        this.employeePositionService = employeePositionService;
        this.employeeService = employeeService;
        this.positionService = positionService;

        add(employeePositionDTOFormLayout);

        setSizeFull();
        setMargin(true);
        setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, employeePositionDTOFormLayout);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String parameter) {
        if (parameter != null) {
            parameterId = UUID.fromString(parameter);
            employeePositionDTO = employeePositionService.getById(parameterId);
        }

        buildEmployeePositionFormLayout();
    }

    private void buildEmployeePositionFormLayout() {
        // Create the query object that will do the pagination of employee records in the combo box component.
        Query<EmployeeDTO, Void> employeeQuery = new Query<>();

        employeeDTOComboBox = new ComboBox<>("Employee");
        employeeDTOComboBox.setItems((employeeDTO, filterString) -> employeeDTO.getEmployeeFullName().toLowerCase().contains(filterString.toLowerCase()),
                employeeService.getAll(employeeQuery.getPage(), employeeQuery.getPageSize()));
        employeeDTOComboBox.setItemLabelGenerator(EmployeeDTO::getEmployeeFullName);
        employeeDTOComboBox.setClearButtonVisible(true);
        employeeDTOComboBox.setRequired(true);
        employeeDTOComboBox.setRequiredIndicatorVisible(true);
        if (employeePositionDTO != null) employeeDTOComboBox.setValue(employeePositionDTO.getEmployeeDTO());

        // Create the query object that will do the pagination of position records in the combo box component.
        Query<PositionDTO, Void> positionQuery = new Query<>();

        positionDTOComboBox = new ComboBox<>("Position");
        positionDTOComboBox.setItems((positionDTO, filterString) -> positionDTO.getName().toLowerCase().contains(filterString.toLowerCase()),
                positionService.getAll(positionQuery.getPage(), positionQuery.getPageSize()));
        positionDTOComboBox.setItemLabelGenerator(PositionDTO::getName);
        positionDTOComboBox.setClearButtonVisible(true);
        positionDTOComboBox.setRequired(true);
        positionDTOComboBox.setRequiredIndicatorVisible(true);
        if (employeePositionDTO != null) positionDTOComboBox.setValue(employeePositionDTO.getPositionDTO());

        currentPositionCheckbox = new Checkbox("Is Position Active?");
        if (employeePositionDTO != null) currentPositionCheckbox.setValue(employeePositionDTO.isCurrentPosition());

        Button saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(buttonClickEvent -> {
            saveOrUpdateEmployeePositionDTO();
            saveButton.getUI().ifPresent(ui -> ui.navigate(EmployeePositionListView.class));
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(buttonClickEvent -> cancelButton.getUI().ifPresent(ui -> ui.navigate(EmployeePositionListView.class)));

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(cancelButton, saveButton);
        buttonLayout.setJustifyContentMode(JustifyContentMode.END);
        buttonLayout.setWidth("768px");
        buttonLayout.setPadding(true);

        employeePositionDTOFormLayout.add(employeeDTOComboBox,
                positionDTOComboBox,
                currentPositionCheckbox,
                buttonLayout);
        employeePositionDTOFormLayout.setColspan(currentPositionCheckbox, 2);
        employeePositionDTOFormLayout.setColspan(buttonLayout, 2);
        employeePositionDTOFormLayout.setWidth("768px");
    }

    private void saveOrUpdateEmployeePositionDTO() {
        String loggedInUser = Objects.requireNonNull(SecurityUtil.getAuthenticatedUser()).getUsername();

        if (parameterId != null) {
            employeePositionDTO = employeePositionService.getById(parameterId);
        } else {
            employeePositionDTO = new EmployeePositionDTO();
            employeePositionDTO.setCreatedBy(loggedInUser);
        }

        employeePositionDTO.setEmployeeDTO(employeeDTOComboBox.getValue());
        employeePositionDTO.setPositionDTO(positionDTOComboBox.getValue());
        employeePositionDTO.setCurrentPosition(currentPositionCheckbox.getValue());
        employeePositionDTO.setUpdatedBy(loggedInUser);

        employeePositionService.saveOrUpdate(employeePositionDTO);

        // Show notification message.
        Notification notification = Notification.show("You have successfully saved an employee position.",  5000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}
