package de.unituebingen.jeniffer2.main.util.saveparameterdialog;

import com.airhacks.afterburner.views.FXMLView;

import java.util.function.Function;

public class SaveParameterDialogView extends FXMLView {

    public SaveParameterDialogView(Function<String, Object> injectionContext) {
        super(injectionContext);
    }

}
