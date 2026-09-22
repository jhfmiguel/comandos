import * as React from "react";
export const Label = React.forwardRef<HTMLLabelElement, React.LabelHTMLAttributes<HTMLLabelElement>>(function Label(props, ref) {
  return <label ref={ref} {...props} />;
});
