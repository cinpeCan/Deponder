# Keep XML-inflated DeponderRubberView constructors and public View operator surface for R8/ProGuard consumers.
-keep class com.cinpe.deponder.DeponderRubberView {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
