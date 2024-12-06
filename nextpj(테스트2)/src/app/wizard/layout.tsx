import './wizardstyle.css'

export default function WizardLayout({
    children,
  }: {
    children: React.ReactNode
  }) {
    return (
      <div className="h-full">
        {children}
      </div>
    );
  }