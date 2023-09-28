package de.unituebingen.jeniffer2.main.util.informationdialog;

import com.airhacks.afterburner.views.FXMLView;

import java.util.function.Function;

public class InformationDialogView extends FXMLView {

    public InformationDialogView(Function<String, Object> injectionContext) {
        super(injectionContext);
    }
}
