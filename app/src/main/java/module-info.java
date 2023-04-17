module jp.henry.uke.mask {
    exports jp.henry.uke.mask;
    requires kotlin.stdlib;

    // To use picocli
    // https://picocli.info/#_module_configuration
    opens jp.henry.uke.mask to info.picocli;
    requires info.picocli;
}
