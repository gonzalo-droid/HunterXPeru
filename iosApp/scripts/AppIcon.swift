// Genera iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png (luego: sips a 1024 px y sin alfa, ver README).
import AppKit
// Mismo diseño que androidApp/src/main/res/drawable/ic_launcher_foreground.xml (viewport 108).
let size: CGFloat = 1024
let s = size / 108 * 1.25   // el ícono de iOS no recorta como el adaptativo: se agranda un poco
// Contorno del dibujo: x e y entre 30 y 78,5 → centro en 54,25.
let ox = size / 2 - 54.25 * s, oy = size / 2 - 54.25 * s
let img = NSImage(size: NSSize(width: size, height: size))
img.lockFocus()
let ctx = NSGraphicsContext.current!.cgContext
ctx.setFillColor(CGColor(red: 0x16/255, green: 0x18/255, blue: 0x1B/255, alpha: 1))
ctx.fill(CGRect(x: 0, y: 0, width: size, height: size))
func p(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: ox + x * s, y: size - (oy + y * s)) }
let yellow = CGColor(red: 0xF2/255, green: 0xB9/255, blue: 0x3B/255, alpha: 1)
ctx.setStrokeColor(yellow)
ctx.setLineWidth(6 * s)
ctx.strokeEllipse(in: CGRect(x: p(33, 67).x, y: p(33, 67).y, width: 34 * s, height: 34 * s))
ctx.setLineCap(.round)
ctx.setLineWidth(7 * s)
ctx.move(to: p(63, 63)); ctx.addLine(to: p(75, 75)); ctx.strokePath()
ctx.setFillColor(CGColor(red: 0xED/255, green: 0xEB/255, blue: 0xE6/255, alpha: 1))
ctx.fill(CGRect(x: p(48, 59).x, y: p(48, 59).y, width: 4 * s, height: 18 * s))
ctx.fill(CGRect(x: p(41, 52).x, y: p(41, 52).y, width: 18 * s, height: 4 * s))
img.unlockFocus()
let rep = NSBitmapImageRep(data: img.tiffRepresentation!)!
// iOS exige 1024 px sin transparencia.
try! rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: CommandLine.arguments[1]))
