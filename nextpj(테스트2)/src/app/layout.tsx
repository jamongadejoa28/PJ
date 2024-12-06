export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html className="h-full">
      <head>
        <title>OSM Web Wizard for SUMO</title>
      </head>
      <body className="h-full m-0 p-0">
        {children}
      </body>
    </html>
  )
}