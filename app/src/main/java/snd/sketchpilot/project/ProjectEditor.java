package snd.sketchpilot.project;

import java.io.File;
import java.util.List;

import snd.sketchpilot.blocks.BlockSpecRegistry;
import snd.sketchpilot.project.backup.BackupManager;
import snd.sketchpilot.project.crypto.SketchwareCrypto;
import snd.sketchpilot.project.io.ProjectFileIO;
import snd.sketchpilot.project.io.SectionedFile;
import snd.sketchpilot.project.ir.LogicIr;
import snd.sketchpilot.project.ir.ViewIr;
import snd.sketchpilot.project.ir.ViewIrExtractor;
import snd.sketchpilot.project.ir.ViewIrCompiler;
import snd.sketchpilot.project.ir.LogicIrCompiler;
import snd.sketchpilot.project.transactions.TransactionManager;
import snd.sketchpilot.project.transactions.TransactionPlan;
import snd.sketchpilot.project.validators.IrValidator;
import snd.sketchpilot.view.WidgetRegistry;

/**
 * Facade used by tools and the agent: extract typed IR, validate, compile and
 * commit through a backed-up transaction. No Android dependencies.
 */
public final class ProjectEditor {

    private final SketchwarePaths paths;
    private final SketchwareCrypto crypto;
    private final OfflineRepository repository;
    private final ProjectFileIO io;
    private final TransactionManager transactions;
    private final WidgetRegistry widgets = new WidgetRegistry();
    private final BlockSpecRegistry specs = new BlockSpecRegistry();
    private final IrValidator validator;

    public ProjectEditor(SketchwarePaths paths, SketchwareCrypto crypto) {
        this.paths = paths;
        this.crypto = crypto;
        this.repository = new OfflineRepository(paths, crypto);
        this.io = new ProjectFileIO(crypto);
        this.transactions = new TransactionManager(paths, io, new BackupManager(paths));
        this.validator = new IrValidator(widgets, specs);
    }

    public OfflineRepository getRepository() {
        return repository;
    }

    public BlockSpecRegistry getBlockSpecs() {
        return specs;
    }

    public WidgetRegistry getWidgets() {
        return widgets;
    }

    public TransactionManager getTransactions() {
        return transactions;
    }

    public List<ViewIr> getViewIr(String scId) throws RepositoryException {
        SectionedFile view = repository.readSectioned(scId, "view");
        return ViewIrExtractor.extract(view, repository.getRootLayoutsJson(scId));
    }

    public TransactionManager.Result applyViewIr(String scId, ViewIr ir) {
        List<String> errors = validator.validate(ir);
        if (!errors.isEmpty()) {
            return new TransactionManager.Result(false, join(errors));
        }
        try {
            SectionedFile current = repository.readSectioned(scId, "view");
            SectionedFile compiled = new ViewIrCompiler(widgets).compile(current, ir);
            TransactionPlan plan = new TransactionPlan("view:" + ir.getLayoutName());
            plan.put(paths.dataFile(scId, "view"), cryptoEncrypt(compiled.serialize()));
            String rootsJson = repository.getRootLayoutsJson(scId);
            org.json.JSONObject roots = snd.sketchpilot.util.JsonUtil.tryParseObject(rootsJson);
            if (roots == null) {
                roots = new org.json.JSONObject();
            }
            org.json.JSONObject root = new org.json.JSONObject();
            root.put("attributes", ir.getRootAttributes());
            root.put("class_name", "LinearLayout");
            roots.put(ir.getLayoutName(), root);
            plan.put(paths.dataFile(scId, "view_root"), roots.toString().getBytes("UTF-8"));
            return transactions.commit(scId, plan);
        } catch (Throwable t) {
            return new TransactionManager.Result(false, "compile failed: " + t.getMessage());
        }
    }

    public TransactionManager.Result applyLogicIr(String scId, LogicIr ir) {
        List<String> errors = validator.validate(ir);
        if (!errors.isEmpty()) {
            return new TransactionManager.Result(false, join(errors));
        }
        try {
            SectionedFile current = repository.readSectioned(scId, "logic");
            SectionedFile compiled = new LogicIrCompiler(specs).compile(current, ir);
            TransactionPlan plan = new TransactionPlan("logic:" + ir.getSectionName());
            plan.put(paths.dataFile(scId, "logic"), cryptoEncrypt(compiled.serialize()));
            return transactions.commit(scId, plan);
        } catch (Throwable t) {
            return new TransactionManager.Result(false, "compile failed: " + t.getMessage());
        }
    }

    private byte[] cryptoEncrypt(String text) throws Exception {
        return crypto.encrypt(text);
    }

    private static String join(List<String> errors) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < errors.size(); i++) {
            if (i > 0) {
                out.append("; ");
            }
            out.append(errors.get(i));
        }
        return out.toString();
    }
}
